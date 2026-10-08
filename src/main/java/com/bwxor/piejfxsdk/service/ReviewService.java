package com.bwxor.piejfxsdk.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.bwxor.piejfxsdk.dto.ToolUseDisplayItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Compile check. The model chooses the compile command (one small call, no tools);
 * Java runs it and decides pass/fail from the exit code. The chosen command is cached
 * per workspace, so later checks and fix rounds usually make no model call at all.
 */
public class ReviewService {

    public enum Status { PASSED, FAILED, SKIPPED }

    /** output: trimmed compiler output on FAILED, a short reason on SKIPPED. */
    public record CompileResult(Status status, String command, String output) {}

    // A cheap model is plenty for picking a build command. Set to null to use the agent's model.
    private static final String REVIEW_MODEL = "anthropic/claude-haiku-4.5";
    private static final long MAX_OUTPUT_TOKENS = 150;
    private static final long COMMAND_TIMEOUT_SECONDS = 300;
    private static final int MAX_OUTPUT_CHARS = 4000;
    private static final int MAX_LISTED_FILES = 150;
    private static final String NO_COMMAND = "NONE";

    // Changes to these alone never need a compile.
    private static final Set<String> NON_CODE_EXTENSIONS = Set.of(".md", ".txt", ".gitignore", ".png", ".jpg", ".svg");

    private static final String SYSTEM_PROMPT = """
            You choose the compile-only command for a project. Reply with the command only:
            one line, no explanation, no code fences. It runs from the workspace root.
            Prefer the project's wrapper (mvnw, gradlew) if present. Use quiet, non-interactive
            flags and do not run tests. Examples: mvn -q -B compile, ./gradlew classes -q,
            npx --no-install tsc --noEmit, cargo check -q, go build ./...
            If nothing in the project needs compiling, reply NONE.
            """;

    private final AnthropicClient client;
    private final String model;

    // key: workspace root + top-level file names -> chosen command
    private final Map<String, String> commandCache = new ConcurrentHashMap<>();

    public ReviewService(AnthropicClient client, String model) {
        this.client = client;
        this.model = REVIEW_MODEL != null ? REVIEW_MODEL : model;
    }

    /** root: the workspace folder; the compile command runs there. */
    public CompileResult check(Path root, Collection<String> changedFiles, Consumer<ToolUseDisplayItem> toolUseDisplayFunction) throws IOException, InterruptedException {
        if (!changedFiles.isEmpty() && changedFiles.stream().allMatch(ReviewService::isNonCode)) {
            return new CompileResult(Status.SKIPPED, null, "only non-code files changed");
        }

        String listing = topLevelListing(root);
        String cacheKey = root.toAbsolutePath() + "|" + listing;

        String command = commandCache.get(cacheKey);
        if (command == null) {
            command = chooseCommand(listing, changedFiles, null);
        }

        for (int attempt = 0; attempt < 2; attempt++) {
            if (command.isBlank() || command.equalsIgnoreCase(NO_COMMAND)) {
                commandCache.put(cacheKey, NO_COMMAND);
                return new CompileResult(Status.SKIPPED, null, "no compiler applies to this project");
            }

            toolUseDisplayFunction.accept(new ToolUseDisplayItem("❯ compile check", command));
            RunResult run = run(root, command);

            if (run.exitCode == 0) {
                commandCache.put(cacheKey, command);
                return new CompileResult(Status.PASSED, command, "");
            }
            if (!run.commandNotFound() || attempt == 1) {
                // A real compile failure (or a second unusable command): hand the output to the agent.
                commandCache.put(cacheKey, command);
                return new CompileResult(Status.FAILED, command, trimTail(run.output));
            }

            // The command itself could not run (missing tool or wrapper): ask once more, showing why.
            commandCache.remove(cacheKey);
            command = chooseCommand(listing, changedFiles,
                    "`" + command + "` could not run:\n" + trimTail(run.output));
        }
        throw new IllegalStateException("unreachable");
    }

    /** Forget cached commands, e.g. when the user opens another folder. */
    public void clearCache() {
        commandCache.clear();
    }

    private String chooseCommand(String listing, Collection<String> changedFiles, String previousFailure) {
        String os = System.getProperty("os.name");
        StringBuilder user = new StringBuilder()
                .append("OS: ").append(os).append('\n')
                .append("Top-level files:\n").append(listing).append('\n')
                .append("Changed files:\n")
                .append(changedFiles.isEmpty() ? "(none tracked)" : String.join("\n", changedFiles));
        if (previousFailure != null) {
            user.append("\n\n").append(previousFailure).append("\nChoose a different command.");
        }

        BetaMessage response = client.beta().messages().create(MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_OUTPUT_TOKENS)
                .system(SYSTEM_PROMPT)
                .addUserMessage(user.toString())
                .build());

        String text = response.content().stream()
                .flatMap(cb -> cb.text().stream())
                .map(t -> t.text())
                .collect(Collectors.joining())
                .strip();

        // Be forgiving if the model wraps the command anyway.
        text = text.replace("```", "").strip();
        int newline = text.indexOf('\n');
        if (newline >= 0) {
            text = text.lines().filter(l -> !l.isBlank() && !l.strip().matches("^[a-z]+$")).findFirst().orElse("").strip();
        }
        if (text.startsWith("`") && text.endsWith("`") && text.length() > 1) {
            text = text.substring(1, text.length() - 1).strip();
        }
        return text;
    }

    private record RunResult(int exitCode, String output) {
        boolean commandNotFound() {
            String lower = output.toLowerCase(Locale.ROOT);
            return exitCode == 127 || exitCode == 9009
                    || lower.contains("command not found")
                    || lower.contains("is not recognized as an internal or external command")
                    || lower.contains("no such file or directory");
        }
    }

    private static RunResult run(Path root, String command) throws IOException, InterruptedException {
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        ProcessBuilder pb = windows
                ? new ProcessBuilder("cmd.exe", "/c", command)
                : new ProcessBuilder("sh", "-c", command);

        Path log = Files.createTempFile("pie-compile", ".log");
        try {
            Process process = pb.directory(root.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(log.toFile())
                    .start();
            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new RunResult(-1, "Compile command timed out after " + COMMAND_TIMEOUT_SECONDS + "s.");
            }
            return new RunResult(process.exitValue(), Files.readString(log, StandardCharsets.UTF_8));
        } finally {
            Files.deleteIfExists(log);
        }
    }

    private static String topLevelListing(Path root) throws IOException {
        try (Stream<Path> entries = Files.list(root)) {
            return entries
                    .map(p -> p.getFileName().toString() + (Files.isDirectory(p) ? "/" : ""))
                    .filter(name -> !name.startsWith(".") || name.equals(".mvn/"))
                    .sorted()
                    .limit(MAX_LISTED_FILES)
                    .collect(Collectors.joining("\n"));
        }
    }

    private static boolean isNonCode(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return NON_CODE_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    /** Compiler errors and the build summary are printed last, so keep the tail. */
    private static String trimTail(String text) {
        if (text.length() <= MAX_OUTPUT_CHARS) {
            return text;
        }
        return "[... showing last " + MAX_OUTPUT_CHARS + " characters ...]\n"
                + text.substring(text.length() - MAX_OUTPUT_CHARS);
    }
}
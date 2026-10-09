package com.bwxor.piejfxsdk.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.helpers.BetaMessageAccumulator;
import com.anthropic.models.beta.messages.*;
import com.bwxor.piejfxsdk.dto.DelegateTaskReturnItem;
import com.bwxor.piejfxsdk.dto.Session;
import com.bwxor.piejfxsdk.dto.ToolUseDisplayItem;
import com.bwxor.piejfxsdk.factory.MessageFactory;
import com.bwxor.piejfxsdk.state.OpenedFolderState;
import com.bwxor.piejfxsdk.state.ServiceState;
import com.bwxor.piejfxsdk.state.SessionState;
import com.bwxor.piejfxsdk.state.UIState;
import com.bwxor.piejfxsdk.tool.*;
import com.bwxor.piejfxsdk.type.Role;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class ClaudeMessageService {
    private static final String EMPTY_STRING = "";
    private static final String NEWLINE_STRING = "\n";
    private static final Integer TOKEN_AMOUNT_TO_START_CLEANING = 100_000;
    private static final Integer TOKEN_AMOUNT_TO_CLEAR_AT_LEAST = 20_000;
    // REVIEW: how many review -> fix cycles before remaining issues are just shown to the user
    private static final int MAX_REVIEW_ROUNDS = 2;
    private static final String SYSTEM_PROMPT = """
            You are a coding agent working inside the user's workspace in the Pie editor.
            
            Files:
            - Create or modify whatever project files the task needs, including
              build and config files such as pom.xml, build.gradle or package.json.
            - The only Markdown file you may create is CHANGES.md. Do not create other
              documentation or guideline files such as README.md, GUIDE.md, SETUP.md,
              USAGE.md or NOTES.md, unless the user explicitly asks for one.
            - Put any explanations, instructions or usage notes in your answer to
              the user instead of in separate files.
            
            When you have finished making changes:
            - Write CHANGES.md in the workspace root. List each file you created or
              modified, with a one-line description of the change. If CHANGES.md
              already exists, replace its contents.
            - Then give the user a short summary of what you did.
            
            Read files before modifying them. Keep answers concise.
            """;

    private AnthropicClient client;
    private String model;
    // REVIEW
    private ReviewService reviewService;
    private boolean reviewEnabled = true;

    public ClaudeMessageService(String apiKey, String model) {
        this.client = AnthropicOkHttpClient.builder()
                .authToken(apiKey)
                .baseUrl("https://openrouter.ai/api")
                .putHeader("X-Title", "PieJFX")
                .build();
        this.model = model;
        // REVIEW
        this.reviewService = new ReviewService(client, model);
    }

    public void sendAgentic(String prompt, int maxIterations, Consumer<String> displayFunction, Consumer<ToolUseDisplayItem> toolUseDisplayFunction, Consumer<Boolean> toggleWorkingFunction, Consumer<Double> progressBarFunction, Consumer<Long> usedTokensFunction, Consumer<String> errorFunction, Runnable openFolderRunnable) {
        boolean isClaude = model.contains("anthropic/");

        try {
            SessionState sessionState = SessionState.instance;

            Session currentSession = sessionState.getSessions().get(sessionState.getCurrentSessionIndex());

            currentSession.getMessages().add(new com.bwxor.piejfxsdk.dto.Message(prompt, Role.USER));
            System.out.println("[" + sessionState.getCurrentSessionIndex() + "] User: " + prompt);

            if (currentSession.getConversation() == null) {
                currentSession.setConversation(MessageCreateParams.builder()
                        .model(this.model)
                        .maxTokens(16000)
                        .system(SYSTEM_PROMPT)
                        .addTool(ListWorkspaceFiles.class)
                        .addTool(ReadWorkspaceFile.class)
                        .addTool(WriteWorkspaceFile.class)
                        .addTool(ExecuteTerminalCommand.class)
                        .addTool(DelegateTask.class)
                        .putAdditionalBodyProperty("session_id", JsonValue.from(currentSession.getId())));

                if (isClaude) {
                    currentSession.getConversation()
                            .putAdditionalBodyProperty("context_management", JsonValue.from(Map.of(
                                    "edits", List.of(Map.of(
                                            "type", "clear_tool_uses_20250919",
                                            "trigger", Map.of("type", "input_tokens", "value", TOKEN_AMOUNT_TO_START_CLEANING),
                                            "keep", Map.of("type", "tool_uses", "value", 4),
                                            "clear_at_least", Map.of("type", "input_tokens", "value", TOKEN_AMOUNT_TO_CLEAR_AT_LEAST),
                                            "clear_tool_inputs", true
                                    ))
                            )))
                            .putAdditionalBodyProperty("cache_control", JsonValue.from(Map.of("type", "ephemeral")))
                            .putAdditionalHeader("anthropic-beta", "context-management-2025-06-27");
                }
            }

            currentSession.getConversation().addUserMessage(prompt);

            toggleWorkingFunction.accept(true);

            var changedFiles = new HashSet<String>();
            runLoop(currentSession, isClaude, reviewEnabled, changedFiles, maxIterations, displayFunction, toolUseDisplayFunction, progressBarFunction, usedTokensFunction, openFolderRunnable);

        } catch (Exception e) {
            e.printStackTrace();
            errorFunction.accept(e.getMessage());
        } finally {
            toggleWorkingFunction.accept(false);
        }
    }

    public void runLoop(Session currentSession, boolean isClaude, boolean reviewEnabled, Set<String> changedFiles, int maxIterations, Consumer<String> displayFunction, Consumer<ToolUseDisplayItem> toolUseDisplayFunction, Consumer<Double> progressBarFunction, Consumer<Long> usedTokensFunction, Runnable openFolderRunnable) {
        ServiceState serviceState = ServiceState.instance;
        OpenedFolderState openedFolderState = OpenedFolderState.instance;

        File openedFolder = openedFolderState.getOpenedFolder();

        // REVIEW: files written in this task, tracked from actual tool calls (not from CHANGES.md)
        int reviewRounds = 0;

        for (int i = 0; i < maxIterations; i++) {
            if (i > 0) {
                displayFunction.accept(NEWLINE_STRING);
            }

            BetaMessageAccumulator messageAccumulator = BetaMessageAccumulator.create();
            BetaStopReason[] stopReason = {null};
            Exception[] accumulateError = {null};

            try (var streamResponse = client.beta().messages()
                    .createStreaming(currentSession.getConversation().build())) {
                streamResponse.stream()
                        .peek(event -> {
                            event.messageDelta().ifPresent(d -> stopReason[0] = d.delta().stopReason().orElse(null));
                            if (accumulateError[0] == null) {
                                try {
                                    messageAccumulator.accumulate(event);
                                } catch (Exception ex) {
                                    accumulateError[0] = ex;
                                }
                            }
                        })
                        .flatMap(event -> event.contentBlockDelta().stream())
                        .flatMap(deltaEvent -> deltaEvent.delta().text().stream())
                        .forEach(textDelta -> displayFunction.accept(textDelta.text()));
            }

            if (accumulateError[0] != null) {
                System.out.println("Tool JSON broken. stop_reason=" + stopReason[0]);
                // recover as in the previous snippet: placeholder assistant msg + "retry smaller" user msg, continue
            }

            displayFunction.accept(NEWLINE_STRING);

            var response = messageAccumulator.message();

            var limit = isClaude ? TOKEN_AMOUNT_TO_START_CLEANING : serviceState.getModelInfoService().getContextLength(model);
            var usage = response.usage();
            var usedTokens = usage.inputTokens() + usage.cacheReadInputTokens().orElse(0L) + usage.cacheCreationInputTokens().orElse(0L);
            double progress = Math.min(1.0, (double) usedTokens / limit);
            progressBarFunction.accept(progress);
            usedTokensFunction.accept(usedTokens);

            response.content().forEach(cb -> {
                if (cb.toolUse().isPresent()) {
                    System.out.println("Tool use: " + cb.toolUse().get().name() + " input=" + cb.toolUse().get()._input());
                }
            });

            var toolUseBlocks = response.content().stream()
                    .flatMap(cb -> cb.toolUse().stream())
                    .toList();

            // AI History
            var assistantBlocks = response.content().stream()
                    .map(cb -> {
                        if (cb.toolUse().isPresent()) {
                            var tub = cb.toolUse().get();
                            return BetaContentBlockParam.ofToolUse(
                                    BetaToolUseBlockParam.builder()
                                            .name(tub.name())
                                            .id(tub.id())
                                            .input(tub._input())
                                            .build());
                        } else if (cb.text().isPresent()) {
                            var textBlock = cb.text().get();
                            return BetaContentBlockParam.ofText(
                                    BetaTextBlockParam.builder()
                                            .text(textBlock.text())
                                            .build());
                        } else if (cb.thinking().isPresent()) {
                            var t = cb.thinking().get();
                            return BetaContentBlockParam.ofThinking(BetaThinkingBlockParam.builder()
                                    .thinking(t.thinking()).signature(t.signature()).build());
                        } else if (cb.redactedThinking().isPresent()) {
                            return BetaContentBlockParam.ofRedactedThinking(BetaRedactedThinkingBlockParam.builder()
                                    .data(cb.redactedThinking().get().data()).build());
                        } else {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            boolean hitMaxTokens = response.stopReason()
                    .map(r -> r.equals(BetaStopReason.MAX_TOKENS))
                    .orElse(false);

            if (hitMaxTokens && !toolUseBlocks.isEmpty()) {
                // The last tool call is probably truncated - don't execute it
                displayFunction.accept("Response was cut off (max tokens). Try asking for smaller files.");
                return;
            }

            if (!assistantBlocks.isEmpty()) {
                currentSession.getConversation().addAssistantMessageOfBetaContentBlockParams(assistantBlocks);
            }

            // if no tool use blocks, show only text and end iteration
            if (toolUseBlocks.isEmpty()) {
                var assistantBlocksText = assistantBlocks.stream()
                        .flatMap(cb -> cb.text().stream())
                        .map(BetaTextBlockParam::text)
                        .collect(Collectors.joining()).strip();

                // REVIEW: before accepting "done", check that the project compiles
                if (reviewEnabled && !changedFiles.isEmpty()) {
                    boolean sentBackForFixes = review(openedFolder, changedFiles, reviewRounds,
                            currentSession.getConversation(), assistantBlocks.isEmpty(),
                            displayFunction, toolUseDisplayFunction);
                    if (sentBackForFixes) {
                        reviewRounds++;
                        continue;
                    }
                }

                currentSession.getMessages().add(new com.bwxor.piejfxsdk.dto.Message(assistantBlocksText, Role.ASSISTANT));
                return;
            }

            // REVIEW: remember which files the agent wrote
            toolUseBlocks.stream()
                    .filter(t -> "write_workspace_file".equals(t.name()))
                    .map(ClaudeMessageService::writtenPath)
                    .filter(Objects::nonNull)
                    .forEach(changedFiles::add);

            // Application history
            var resultBlocks = toolUseBlocks.stream()
                    .map(tub -> BetaContentBlockParam.ofToolResult(
                            BetaToolResultBlockParam.builder()
                                    .toolUseId(tub.id())
                                    .contentAsJson(callTool(tub, changedFiles, toolUseDisplayFunction, openFolderRunnable))
                                    .build()))
                    .toList();

            currentSession.getConversation()
                    .addUserMessageOfBetaContentBlockParams(resultBlocks);
        }

        displayFunction.accept("Hit max iterations without completing.");
    }

    /**
     * REVIEW: runs the compile check. Returns true if compiler errors were sent back to the agent
     * (the loop should continue), false if the task can finish.
     */
    private boolean review(File openedFolder, Set<String> changedFiles, int reviewRounds,
                           MessageCreateParams.Builder conversation, boolean noAssistantMessage,
                           Consumer<String> displayFunction, Consumer<ToolUseDisplayItem> toolUseDisplayFunction) {
        if (openedFolder == null) {
            displayFunction.accept("Compile check skipped: no folder is open." + NEWLINE_STRING);
            return false;
        }

        ReviewService.CompileResult result;
        try {
            displayFunction.accept(NEWLINE_STRING + "I will check if the project compiles." + NEWLINE_STRING);
            result = reviewService.check(openedFolder.toPath(), changedFiles, toolUseDisplayFunction);
        } catch (Exception e) {
            // A broken check must never break the task itself.
            e.printStackTrace();
            displayFunction.accept("Compile check failed: " + e.getMessage() + NEWLINE_STRING);
            return false;
        }

        switch (result.status()) {
            case SKIPPED -> {
                displayFunction.accept("Compile check skipped: " + result.output() + NEWLINE_STRING);
                return false;
            }
            case PASSED -> {
                displayFunction.accept("✓ Review successful. The project compiles." + NEWLINE_STRING);
                return false;
            }
            default -> {
                // FAILED, handled below
            }
        }

        if (reviewRounds < MAX_REVIEW_ROUNDS) {
            displayFunction.accept("✗ Compilation failed. Sending errors back for fixes ("
                    + (reviewRounds + 1) + "/" + MAX_REVIEW_ROUNDS + ")." + NEWLINE_STRING);
            if (noAssistantMessage) {
                // keep user/assistant turns alternating
                conversation.addAssistantMessageOfBetaContentBlockParams(List.of(
                        BetaContentBlockParam.ofText(BetaTextBlockParam.builder().text("Done.").build())));
            }
            conversation.addUserMessage("""
                    `%s` failed. Fix the compilation errors, then update CHANGES.md and give a short summary.
                    
                    %s"""
                    .formatted(result.command(), result.output()));
            return true;
        }

        displayFunction.accept("✗ Still does not compile after " + MAX_REVIEW_ROUNDS + " rounds:"
                + NEWLINE_STRING + result.output() + NEWLINE_STRING);
        return false;
    }

    // REVIEW: path the write tool wrote to. Adjust the accessor to match your WriteWorkspaceFile class.
    private static String writtenPath(BetaToolUseBlock toolUseBlock) {
        try {
            Map<?, ?> input = toolUseBlock.input(Map.class);
            Object path = input != null ? input.get("path") : null;
            return path != null ? path.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Object callTool(BetaToolUseBlock toolUseBlock, Set<String> changedFiles, Consumer<ToolUseDisplayItem> toolUseDisplayFunction, Runnable openFolderRunnable) {
        Class<? extends BuiltInTool<?>> clazz;

        String displaySymbol;

        if ("list_workspace_files".equals(toolUseBlock.name())) {
            clazz = ListWorkspaceFiles.class;
            displaySymbol = "\uD83D\uDCC2";
        } else if ("read_workspace_file".equals(toolUseBlock.name())) {
            clazz = ReadWorkspaceFile.class;
            displaySymbol = "\uD83D\uDCC4";
        } else if ("write_workspace_file".equals(toolUseBlock.name())) {
            clazz = WriteWorkspaceFile.class;
            displaySymbol = "✏\uFE0F";
        } else if ("execute_terminal_command".equals(toolUseBlock.name())) {
            clazz = ExecuteTerminalCommand.class;
            displaySymbol = "\uD83D\uDCBB";
        } else if ("delegate_task".equals(toolUseBlock.name())) {
            clazz = DelegateTask.class;
            displaySymbol = "\uD83E\uDD16";
        } else {
            throw new IllegalArgumentException("Unknown tool: " + toolUseBlock.name());
        }

        boolean needsApproval = "write_workspace_file".equals(toolUseBlock.name())
                || "execute_terminal_command".equals(toolUseBlock.name());

        if (needsApproval && !askApproval(displaySymbol + " " + toolUseBlock.name(), toolUseBlock._input().toString())) {
            return "The user denied this action. Do not retry it; continue without it or ask the user how to proceed.";
        }

        var tool = toolUseBlock.input(clazz);
        toolUseDisplayFunction.accept(new ToolUseDisplayItem(displaySymbol + " " + toolUseBlock.name(), toolUseBlock._input().toString()));
        var result = tool != null ? tool.execute() : EMPTY_STRING;

        if ("write_workspace_file".equals(toolUseBlock.name()) || "execute_terminal_command".equals(toolUseBlock.name())) {
            openFolderRunnable.run();
        }

        if ("delegate_task".equals(toolUseBlock.name()) && (result instanceof DelegateTaskReturnItem returnItem)) {
            changedFiles.addAll(returnItem.changedItems());
            return returnItem.content();
        }

        return result;
    }

    private static boolean askApproval(String title, String details) {
        UIState uiState = UIState.instance;

        var answer = new CompletableFuture<Boolean>();
        Platform.runLater(
                () -> uiState.getMessageHistoryVBox().getChildren()
                .add(MessageFactory.createApprovalMessage(title, details, answer, () -> uiState.getMessageHistoryVBox().getChildren().removeLast())));
        return answer.join();
    }
}
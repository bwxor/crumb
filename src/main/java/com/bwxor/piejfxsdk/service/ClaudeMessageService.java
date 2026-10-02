package com.bwxor.piejfxsdk.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.helpers.BetaMessageAccumulator;
import com.anthropic.models.beta.messages.*;
import com.bwxor.piejfxsdk.dto.Session;
import com.bwxor.piejfxsdk.dto.ToolUseDisplayItem;
import com.bwxor.piejfxsdk.state.ServiceState;
import com.bwxor.piejfxsdk.state.SessionState;
import com.bwxor.piejfxsdk.tool.*;
import com.bwxor.piejfxsdk.type.Role;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class ClaudeMessageService {
    private static final String EMPTY_STRING = "";
    private static final String NEWLINE_STRING = "\n";
    private static final Integer TOKEN_AMOUNT_TO_START_CLEANING = 10_000;
    private static final Integer TOKEN_AMOUNT_TO_CLEAR_AT_LEAST = 2000;
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

    public ClaudeMessageService(String apiKey, String model) {
        this.client = AnthropicOkHttpClient.builder()
                .authToken(apiKey)
                .baseUrl("https://openrouter.ai/api")
                .putHeader("X-Title", "PieJFX")
                .build();
        this.model = model;
    }

    public void sendAgentic(String prompt, int maxIterations, Consumer<String> displayFunction, Consumer<ToolUseDisplayItem> toolUseDisplayFunction, Consumer<Boolean> toggleWorkingFunction, Consumer<Double> progressBarFunction, Consumer<Long> usedTokensFunction, Consumer<String> errorFunction, Runnable openFolderRunnable) {
        ServiceState serviceState = ServiceState.instance;

        boolean isClaude = model.contains("anthropic/");

        try {
            SessionState sessionState = SessionState.instance;

            Session currentSession = sessionState.getSessions().get(sessionState.getCurrentSessionIndex());

            currentSession.getMessages().add(new com.bwxor.piejfxsdk.dto.Message(prompt, Role.USER));
            System.out.println("[" + sessionState.getCurrentSessionIndex() + "] User: " + prompt);

            if (currentSession.getConversation() == null) {
                currentSession.setConversation(com.anthropic.models.beta.messages.MessageCreateParams.builder()
                        .model(this.model)
                        .maxTokens(16000)
                        .system(SYSTEM_PROMPT)
                        .addTool(ListWorkspaceFiles.class)
                        .addTool(ReadWorkspaceFile.class)
                        .addTool(WriteWorkspaceFile.class)
                        .addTool(ExecuteTerminalCommand.class)
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

            // todo for debugging - remove later
            System.out.println(currentSession.getConversation().build());

            toggleWorkingFunction.accept(true);

            for (int i = 0; i < maxIterations; i++) {
                if (i > 0) {
                    displayFunction.accept(NEWLINE_STRING);
                }
                var streamResponse = client.beta().messages().createStreaming(currentSession.getConversation().build());

                BetaMessageAccumulator messageAccumulator = BetaMessageAccumulator.create();

                streamResponse.stream()
                        .peek(messageAccumulator::accumulate)
                        .flatMap(event -> event.contentBlockDelta().stream())
                        .flatMap(deltaEvent -> deltaEvent.delta().text().stream())
                        .forEach(textDelta -> displayFunction.accept(textDelta.text()));

                displayFunction.accept(NEWLINE_STRING);

                var response = messageAccumulator.message();

                var limit = isClaude ? TOKEN_AMOUNT_TO_START_CLEANING : serviceState.getModelInfoService().getContextLength(model);
                var usage = response.usage();
                var usedTokens = usage.inputTokens() + usage.cacheReadInputTokens().orElse(0L) + usage.cacheCreationInputTokens().orElse(0L);
                double progress = Math.min(1.0, (double) usedTokens / limit);
                progressBarFunction.accept(progress);
                usedTokensFunction.accept(usedTokens);

                // ToDo: For debug purposes, until the plugin becomes stable
                System.out.println("Stop reason: " + response.stopReason());
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
                    currentSession.getMessages().add(new com.bwxor.piejfxsdk.dto.Message(assistantBlocksText, Role.ASSISTANT));
                    return;
                }

                // Application history
                var resultBlocks = toolUseBlocks.stream()
                        .map(tub -> BetaContentBlockParam.ofToolResult(
                                BetaToolResultBlockParam.builder()
                                        .toolUseId(tub.id())
                                        .contentAsJson(callTool(tub, toolUseDisplayFunction, openFolderRunnable))
                                        .build()))
                        .toList();

                currentSession.getConversation()
                        .addUserMessageOfBetaContentBlockParams(resultBlocks);
            }

            displayFunction.accept("Hit max iterations without completing.");
        } catch (Exception e) {
            e.printStackTrace();
            errorFunction.accept(e.getMessage());
        } finally {
            toggleWorkingFunction.accept(false);
        }
    }

    private static Object callTool(BetaToolUseBlock toolUseBlock, Consumer<ToolUseDisplayItem> toolUseDisplayFunction, Runnable openFolderRunnable) {
        Class<? extends BuiltInTool<?>> clazz;

        Character displayChar;

        if ("list_workspace_files".equals(toolUseBlock.name())) {
            clazz = ListWorkspaceFiles.class;
            displayChar = '☰';
        } else if ("read_workspace_file".equals(toolUseBlock.name())) {
            clazz = ReadWorkspaceFile.class;
            displayChar = '⎘';
        } else if ("write_workspace_file".equals(toolUseBlock.name())) {
            clazz = WriteWorkspaceFile.class;
            displayChar = '✎';
        } else if ("execute_terminal_command".equals(toolUseBlock.name())) {
            clazz = ExecuteTerminalCommand.class;
            displayChar = '❯';
        } else {
            throw new IllegalArgumentException("Unknown tool: " + toolUseBlock.name());
        }

        var tool = toolUseBlock.input(clazz);
        toolUseDisplayFunction.accept(new ToolUseDisplayItem(displayChar + " " + toolUseBlock.name(), toolUseBlock._input().toString()));
        var result = tool != null ? tool.execute() : EMPTY_STRING;

        if ("write_workspace_file".equals(toolUseBlock.name()) || "execute_terminal_command".equals(toolUseBlock.name())) {
            openFolderRunnable.run();
        }
        return result;
    }
}

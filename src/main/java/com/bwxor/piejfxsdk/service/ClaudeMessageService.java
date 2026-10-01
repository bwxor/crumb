package com.bwxor.piejfxsdk.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.helpers.BetaMessageAccumulator;
import com.anthropic.models.beta.messages.*;
import com.anthropic.models.messages.Model;
import com.bwxor.piejfxsdk.dto.Session;
import com.bwxor.piejfxsdk.dto.ToolUseDisplayItem;
import com.bwxor.piejfxsdk.state.SessionState;
import com.bwxor.piejfxsdk.tool.*;
import com.bwxor.piejfxsdk.type.ClaudeModel;
import com.bwxor.piejfxsdk.type.Role;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class ClaudeMessageService {
    private static final String EMPTY_STRING = "";
    private static final String NEWLINE_STRING = "\n";
    private static final Integer TOKEN_AMOUNT_TO_START_CLEANING = 5000;
    private static final Integer TOKEN_AMOUNT_TO_CLEAR_AT_LEAST = 2000;

    private AnthropicClient client;
    private Model model;

    public ClaudeMessageService(String apiKey, ClaudeModel claudeModel) {
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.model = claudeModel.getAnthropicModel();
    }

    public void sendAgentic(String prompt, int maxIterations, Consumer<String> displayFunction, Consumer<ToolUseDisplayItem> toolUseDisplayFunction, Consumer<Boolean> toggleWorkingFunction, Consumer<Double> progressBarFunction, Consumer<Long> usedTokensFunction) {
        try {
            SessionState sessionState = SessionState.instance;

            Session currentSession = sessionState.getSessions().get(sessionState.getCurrentSessionIndex());

            currentSession.getMessages().add(new com.bwxor.piejfxsdk.dto.Message(prompt, Role.USER));
            System.out.println("[" + sessionState.getCurrentSessionIndex() + "] User: " + prompt);

            if (currentSession.getConversation() == null) {
                currentSession.setConversation(com.anthropic.models.beta.messages.MessageCreateParams.builder()
                        .model(this.model)
                        .maxTokens(4096)
                        .addTool(ListWorkspaceFiles.class)
                        .addTool(ReadWorkspaceFile.class)
                        .addTool(WriteWorkspaceFile.class)
                        .addTool(ExecuteTerminalCommand.class)
                        .putAdditionalBodyProperty("cache_control", JsonValue.from(Map.of("type", "ephemeral")))
                        .putAdditionalBodyProperty("context_management", JsonValue.from(Map.of(
                                "edits", List.of(Map.of(
                                        "type", "clear_tool_uses_20250919",
                                        "trigger", Map.of("type", "input_tokens", "value", TOKEN_AMOUNT_TO_START_CLEANING),
                                        "keep", Map.of("type", "tool_uses", "value", 4),
                                        "clear_at_least", Map.of("type", "input_tokens", "value", TOKEN_AMOUNT_TO_CLEAR_AT_LEAST),
                                        "clear_tool_inputs", true
                                ))
                        )))
                        .putAdditionalHeader("anthropic-beta", "context-management-2025-06-27"));
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

                var usage = response.usage();
                var usedTokens = usage.inputTokens() + usage.cacheReadInputTokens().orElse(0L) + usage.cacheCreationInputTokens().orElse(0L);
                double progress = Math.min(1.0, (double) usedTokens / TOKEN_AMOUNT_TO_START_CLEANING);
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
                            } else {
                                throw new IllegalStateException("Unhandled content block type: " + cb);
                            }
                        })
                        .toList();

                if (!assistantBlocks.isEmpty()) {
                    currentSession.getConversation().addAssistantMessageOfBetaContentBlockParams(assistantBlocks);
                }

                if (response.stopReason().equals("max_tokens") && !toolUseBlocks.isEmpty()) {
                    // The last tool call is probably truncated - don't execute it
                    displayFunction.accept("Response was cut off (max tokens). Try asking for smaller files.");
                    return;
                }

                // if no tool use blocks, show only text and end iteration
                if (toolUseBlocks.isEmpty()) {
                    var assistantBlocksText = assistantBlocks.stream()
                            .flatMap(cb -> cb.text().stream())
                            .map(BetaTextBlockParam::text)
                            .collect(Collectors.joining()).strip();
                    toggleWorkingFunction.accept(false);
                    currentSession.getMessages().add(new com.bwxor.piejfxsdk.dto.Message(assistantBlocksText, Role.ASSISTANT));
                    return;
                }

                // Application history
                var resultBlocks = toolUseBlocks.stream()
                        .map(tub -> BetaContentBlockParam.ofToolResult(
                                BetaToolResultBlockParam.builder()
                                        .toolUseId(tub.id())
                                        .contentAsJson(callTool(tub, toolUseDisplayFunction))
                                        .build()))
                        .toList();

                currentSession.getConversation()
                        .addUserMessageOfBetaContentBlockParams(resultBlocks);
            }

            displayFunction.accept("Hit max iterations without completing.");
            toggleWorkingFunction.accept(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Object callTool(BetaToolUseBlock toolUseBlock, Consumer<ToolUseDisplayItem> toolUseDisplayFunction) {
        Class<? extends BuiltInTool<?>> clazz;

        Character displayChar;

        if ("list_workspace_files".equals(toolUseBlock.name())) {
            clazz = ListWorkspaceFiles.class;
            displayChar= '☰';
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
        return tool != null ? tool.execute() : EMPTY_STRING;
    }
}

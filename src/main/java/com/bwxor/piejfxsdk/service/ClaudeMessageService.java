package com.bwxor.piejfxsdk.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.helpers.BetaMessageAccumulator;
import com.anthropic.helpers.MessageAccumulator;
import com.anthropic.models.beta.messages.*;
import com.anthropic.models.messages.*;
import com.anthropic.models.messages.MessageCountTokensParams;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.bwxor.piejfxsdk.dto.Session;
import com.bwxor.piejfxsdk.state.SessionState;
import com.bwxor.piejfxsdk.tool.ListWorkspaceFiles;
import com.bwxor.piejfxsdk.tool.ReadWorkspaceFile;
import com.bwxor.piejfxsdk.type.Role;

import java.util.function.Consumer;
import java.util.stream.Collectors;


public class ClaudeMessageService {
    private static final String EMPTY_STRING = "";
    private static final String NEWLINE_STRING = "\n";

    private AnthropicClient client;
    private Model model;

    public ClaudeMessageService(String apiKey) {
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.model = Model.CLAUDE_HAIKU_4_5;
    }

    public void sendSingleMessage(String prompt, Consumer<String> displayFunction) {
        MessageCreateParams params = MessageCreateParams.builder()
                .maxTokens(1024L)
                .addUserMessage(prompt)
                .model(model)
                .build();

        Message message = client.messages().create(params);

        message.content().forEach(
                contentBlock -> {
                    contentBlock.text().ifPresent(textBlock -> {
                        displayFunction.accept(textBlock.text());
                    });
                }
        );

        System.out.println("Input Tokens: " + message.usage().inputTokens());
        System.out.println("Output Tokens: " + message.usage().outputTokens());
    }

    public void sendSingleMessageStreaming(String prompt, Consumer<String> displayFunction) {
        MessageCreateParams params = MessageCreateParams.builder()
                .maxTokens(1024L)
                .addUserMessage(prompt)
                .model(model)
                .build();

        MessageAccumulator messageAccumulator = MessageAccumulator.create();

        try (StreamResponse<RawMessageStreamEvent> streamResponse = client.messages().createStreaming(params)) {
            streamResponse.stream()
                    .peek(messageAccumulator::accumulate)
                    .flatMap(event -> event.contentBlockDelta().stream())
                    .flatMap(deltaEvent -> deltaEvent.delta().text().stream())
                    .forEach(textDelta -> displayFunction.accept(textDelta.text()));
        }

        var message = messageAccumulator.message();

        System.out.println("Input Tokens: " + message.usage().inputTokens());
        System.out.println("Output Tokens: " + message.usage().outputTokens());

    }


    public long countInputTokens(String prompt) {
        MessageCountTokensParams params = MessageCountTokensParams.builder()
                .addUserMessage(prompt)
                .model(model)
                .build();

        MessageTokensCount result = client.messages().countTokens(params);

        return result.inputTokens();
    }

    public void sendAgentic(String prompt, int maxIterations, Consumer<String> displayFunction) {
        SessionState sessionState = SessionState.instance;

        Session currentSession = sessionState.getSessions().get(sessionState.getCurrentSessionIndex());

        currentSession.getMessages().add(new com.bwxor.piejfxsdk.dto.Message(prompt, Role.USER));
        System.out.println("[" + sessionState.getCurrentSessionIndex() + "] User: " + prompt);

        if (currentSession.getConversation() == null) {
            currentSession.setConversation(com.anthropic.models.beta.messages.MessageCreateParams.builder()
                    .model(Model.CLAUDE_HAIKU_4_5)
                    .maxTokens(4096)
                    .addTool(ListWorkspaceFiles.class)
                    .addTool(ReadWorkspaceFile.class));
        }

        currentSession.getConversation().addUserMessage(prompt);

        // todo for debugging - remove later
        System.out.println(currentSession.getConversation().build());

        for (int i = 0; i < maxIterations; i++) {
            var streamResponse = client.beta().messages().createStreaming(currentSession.getConversation().build());

            BetaMessageAccumulator messageAccumulator = BetaMessageAccumulator.create();

            streamResponse.stream()
                    .peek(messageAccumulator::accumulate)
                    .flatMap(event -> event.contentBlockDelta().stream())
                    .flatMap(deltaEvent -> deltaEvent.delta().text().stream())
                    .forEach(textDelta -> displayFunction.accept(textDelta.text()));

            displayFunction.accept(NEWLINE_STRING);

            var response = messageAccumulator.message();

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
                                    .contentAsJson(callTool(tub))
                                    .build()))
                    .toList();

            currentSession.getConversation()
                    .addUserMessageOfBetaContentBlockParams(resultBlocks);
        }

        displayFunction.accept("Hit max iterations without completing.");
    }

    private static Object callTool(BetaToolUseBlock toolUseBlock) {
        if ("list_workspace_files".equals(toolUseBlock.name())) {
            var tool = toolUseBlock.input(ListWorkspaceFiles.class);
            return tool != null ? tool.execute() : EMPTY_STRING;
        } else if ("read_workspace_file".equals(toolUseBlock.name())) {
            var tool = toolUseBlock.input(ReadWorkspaceFile.class);
            return tool != null ? tool.execute() : EMPTY_STRING;
        } else {
            throw new IllegalArgumentException("Unknown tool: " + toolUseBlock.name());
        }
    }
}

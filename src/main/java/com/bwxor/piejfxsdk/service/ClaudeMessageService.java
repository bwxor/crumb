package com.bwxor.piejfxsdk.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.helpers.MessageAccumulator;
import com.anthropic.models.beta.messages.*;
import com.anthropic.models.messages.*;
import com.anthropic.models.messages.MessageCountTokensParams;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;

import java.util.function.Consumer;


public class ClaudeMessageService {
    private AnthropicClient client;
    private Model model;

    public ClaudeMessageService() {
        this.client = AnthropicOkHttpClient.builder().fromEnv().build();
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
        var createParamsBuilder = com.anthropic.models.beta.messages.MessageCreateParams.builder()
                .model(Model.CLAUDE_OPUS_5)
                .maxTokens(2048)
//                .addTool(GetExchangeRate.class)
                .addUserMessage(prompt);

        for (int i = 0; i < maxIterations; i++) {
            var response = client.beta().messages().create(createParamsBuilder.build());
            var toolUseBlocks = response.content().stream()
                    .flatMap(cb -> cb.toolUse().stream())
                    .toList();

            // if no tool use blocks, show only text and end iteration
            if (toolUseBlocks.isEmpty()) {
                response.content().stream()
                        .flatMap(cb -> cb.text().stream())
                        .forEach(tb -> displayFunction.accept(tb.text()));
                return;
            }

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

            // Application history
            var resultBlocks = toolUseBlocks.stream()
                    .map(tub -> BetaContentBlockParam.ofToolResult(
                            BetaToolResultBlockParam.builder()
                                    .toolUseId(tub.id())
                                    .contentAsJson(callTool(tub))
                                    .build()))
                    .toList();

            createParamsBuilder
                    .addAssistantMessageOfBetaContentBlockParams(assistantBlocks)
                    .addUserMessageOfBetaContentBlockParams(resultBlocks);
        }

        System.out.println("Hit max iterations without completing.");
    }

    private static Object callTool(BetaToolUseBlock toolUseBlock) {
//        if ("get_exchange_rate".equals(toolUseBlock.name())) {
//            GetExchangeRate tool = toolUseBlock.input(GetExchangeRate.class);
//            return tool != null ? tool.execute() : -1.0;
//        }
//        else {
        throw new IllegalArgumentException("Unknown tool: " + toolUseBlock.name());
//        }
    }
}

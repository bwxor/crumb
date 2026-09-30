package com.bwxor.piejfxsdk.dto;

import com.anthropic.models.beta.messages.MessageCreateParams;

import java.util.ArrayList;
import java.util.List;

public class Session {
    private MessageCreateParams.Builder conversation;
    private List<Message> messages = new ArrayList<>();

    public MessageCreateParams.Builder getConversation() {
        return conversation;
    }

    public void setConversation(MessageCreateParams.Builder conversation) {
        this.conversation = conversation;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
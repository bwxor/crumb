package com.bwxor.piejfxsdk.state;

import java.util.List;

public class SessionState {
    private static enum Role {
        USER, ASSISTANT;
    }

    private static class Conversation {
        private Role role;
        private String message;
    }

    private List<Conversation> conversations;

    public static final SessionState instance = new SessionState();

    private SessionState() {}

    public List<Conversation> getConversations() {
        return conversations;
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
    }
}

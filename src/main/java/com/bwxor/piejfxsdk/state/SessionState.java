package com.bwxor.piejfxsdk.state;

import com.bwxor.piejfxsdk.dto.Session;

import java.util.ArrayList;
import java.util.List;

public class SessionState {

    public static final SessionState instance = new SessionState();
    private List<Session> sessions = new ArrayList<>();
    private int currentSessionIndex = -1;

    private SessionState() {}

    public List<Session> getSessions() {
        return sessions;
    }

    public void setSessions(List<Session> sessions) {
        this.sessions = sessions;
    }

    public int getCurrentSessionIndex() {
        return currentSessionIndex;
    }

    public void setCurrentSessionIndex(int currentSessionIndex) {
        this.currentSessionIndex = currentSessionIndex;
    }
}

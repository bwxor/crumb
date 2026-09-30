package com.bwxor.piejfxsdk.state;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MCPServerState {
    public static final MCPServerState instance = new MCPServerState();

    private List<MCPServer> servers = Collections.emptyList();

    private MCPServerState() {}

    public List<MCPServer> getServers() {
        return servers;
    }

    public void setServers(List<MCPServer> servers) {
        this.servers = servers;
    }

    public static MCPServerState fromJson(JSONObject root) {
        MCPServerState state = instance;
        List<MCPServer> list = new ArrayList<>();

        if (root.has("mcpServers")) {
            JSONArray arr = root.getJSONArray("mcpServers");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.optString("name", "");
                String url = obj.optString("url", "");
                list.add(new MCPServer(name, url));
            }
        }

        state.setServers(Collections.unmodifiableList(list));
        return state;
    }

    public static class MCPServer {
        private final String name;
        private final String url;

        public MCPServer(String name, String url) {
            this.name = name;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }
    }
}

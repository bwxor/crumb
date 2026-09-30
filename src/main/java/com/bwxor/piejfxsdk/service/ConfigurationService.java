package com.bwxor.piejfxsdk.service;

import com.bwxor.piejfxsdk.state.ConfigurationState;
import com.bwxor.piejfxsdk.state.MCPServerState;
import com.bwxor.piejfxsdk.type.ClaudeModel;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Paths;

public class ConfigurationService {
    private static final String EMPTY_STRING = "";

    public void loadCredentials() throws IOException {
        ConfigurationState configurationState = ConfigurationState.instance;
        File confFile = new File(Paths.get(ConfigurationState.instance.getConfigurationDirectory().toString(), "config.json").toUri());

        createDirectoryAndFile();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(confFile))) {
            String confContent = bufferedReader.readAllAsString();

            JSONObject jsonObject = new JSONObject(confContent);
            JSONObject config = jsonObject.getJSONObject("config");

            configurationState.setApiKey(config.getString("apiKey"));

            String modelKey = config.optString("model", null);
            configurationState.setModel(ClaudeModel.fromConfigKey(modelKey));

            configurationState.setMaxIterations(config.optInt("maxIterations", 50));
        }
    }

    public void loadMcpServers() throws IOException {
        File mcpFile = new File(Paths.get(ConfigurationState.instance.getConfigurationDirectory().toString(), "mcp_servers.json").toUri());

        createMcpFileIfMissing();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(mcpFile))) {
            String content = bufferedReader.readAllAsString();
            JSONObject jsonObject = new JSONObject(content);
            MCPServerState.fromJson(jsonObject);
        }
    }

    private void createDirectoryAndFile() {
        File confDir = new File(Paths.get(ConfigurationState.instance.getConfigurationDirectory().toString()).toUri());
        File confFile = new File(Paths.get(confDir.toString(), "config.json").toUri());

        if (!confDir.exists()) {
            confDir.mkdirs();
            createFile();
        } else if (!confFile.exists()) {
            createFile();
        }
    }

    private void createFile() {
        File confDir = new File(Paths.get(ConfigurationState.instance.getConfigurationDirectory().toString()).toUri());
        File confFile = new File(Paths.get(confDir.toString(), "config.json").toUri());

        try {
            confFile.createNewFile();

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(confFile));

            JSONObject root = new JSONObject();
            JSONObject config = new JSONObject();
            root.put("config", config);

            config.put("apiKey", EMPTY_STRING);
            config.put("model", ClaudeModel.HAIKU.getConfigKey());
            config.put("maxIterations", 50);

            bufferedWriter.write(root.toString());
            bufferedWriter.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createMcpFileIfMissing() {
        File mcpFile = new File(Paths.get(ConfigurationState.instance.getConfigurationDirectory().toString(), "mcp_servers.json").toUri());

        if (!mcpFile.exists()) {
            try {
                mcpFile.createNewFile();

                JSONObject root = new JSONObject();
                JSONArray servers = new JSONArray();
                root.put("mcpServers", servers);

                try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(mcpFile))) {
                    bufferedWriter.write(root.toString(2));
                    bufferedWriter.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

package com.bwxor.piejfxsdk.service;

import com.bwxor.piejfxsdk.state.ConfigurationState;
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

            bufferedWriter.write(root.toString());
            bufferedWriter.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
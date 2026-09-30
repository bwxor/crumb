package com.bwxor.piejfxsdk.state;

import com.bwxor.piejfxsdk.type.ClaudeModel;

import java.nio.file.Path;

public class ConfigurationState {
    private Path configurationDirectory;
    private String apiKey;
    private ClaudeModel model = ClaudeModel.HAIKU;
    private int maxIterations = 50;
    public static final ConfigurationState instance = new ConfigurationState();
    private ConfigurationState() {}

    public Path getConfigurationDirectory() {
        return configurationDirectory;
    }

    public void setConfigurationDirectory(Path configurationDirectory) {
        this.configurationDirectory = configurationDirectory;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public ClaudeModel getModel() {
        return model;
    }

    public void setModel(ClaudeModel model) {
        this.model = model;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }
}

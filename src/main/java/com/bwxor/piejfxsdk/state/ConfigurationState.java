package com.bwxor.piejfxsdk.state;

import java.nio.file.Path;

public class ConfigurationState {
    private Path configurationDirectory;
    private String apiKey;
    private String model = "anthropic/claude-haiku-4.5";
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

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }
}

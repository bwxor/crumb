package com.bwxor.piejfxsdk.state;

import java.nio.file.Path;

public class ConfigurationState {
    private Path configurationDirectory;
    private String apiKey;
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
}

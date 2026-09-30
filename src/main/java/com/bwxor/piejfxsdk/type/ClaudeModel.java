package com.bwxor.piejfxsdk.type;

import com.anthropic.models.messages.Model;

public enum ClaudeModel {
    HAIKU("haiku", Model.CLAUDE_HAIKU_4_5),
    SONNET("sonnet", Model.CLAUDE_SONNET_4_5),
    OPUS("opus", Model.CLAUDE_OPUS_4_5);

    private final String configKey;
    private final Model anthropicModel;

    ClaudeModel(String configKey, Model anthropicModel) {
        this.configKey = configKey;
        this.anthropicModel = anthropicModel;
    }

    public String getConfigKey() {
        return configKey;
    }

    public Model getAnthropicModel() {
        return anthropicModel;
    }

    public static ClaudeModel fromConfigKey(String key) {
        if (key != null) {
            String normalised = key.trim().toLowerCase();
            for (ClaudeModel m : values()) {
                if (m.configKey.equals(normalised)) {
                    return m;
                }
            }
        }
        return HAIKU;
    }
}

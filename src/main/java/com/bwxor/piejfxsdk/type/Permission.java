package com.bwxor.piejfxsdk.type;

import java.util.Locale;

public enum Permission {
    ALLOW,
    DENY,
    PROMPT;

    /** Parses "allow", "deny" or "prompt" (case-insensitive). Anything else falls back to PROMPT, the safe choice. */
    public static Permission fromString(String value) {
        if (value == null) {
            return PROMPT;
        }
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "allow" -> ALLOW;
            case "deny" -> DENY;
            default -> PROMPT;
        };
    }

    public String toJsonValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}

package com.agentguard.common.enums;

import java.util.Locale;

public enum SandboxMode {
    READ_ONLY,
    WORKSPACE_WRITE,
    DANGER_FULL_ACCESS;

    public static SandboxMode fromCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Sandbox mode cannot be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (SandboxMode mode : values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unsupported sandboxMode, expected one of: READ_ONLY, WORKSPACE_WRITE, DANGER_FULL_ACCESS");
    }
}

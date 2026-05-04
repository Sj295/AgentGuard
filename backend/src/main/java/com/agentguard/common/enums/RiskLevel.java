package com.agentguard.common.enums;

import java.util.Locale;

public enum RiskLevel {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int rank;

    RiskLevel(int rank) {
        this.rank = rank;
    }

    public static RiskLevel fromCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Risk level cannot be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (RiskLevel riskLevel : values()) {
            if (riskLevel.name().equals(normalized)) {
                return riskLevel;
            }
        }
        throw new IllegalArgumentException("Unsupported riskLevel, expected one of: LOW, MEDIUM, HIGH, CRITICAL");
    }

    public static RiskLevel fromScore(int score) {
        if (score <= 30) {
            return LOW;
        }
        if (score <= 60) {
            return MEDIUM;
        }
        if (score <= 85) {
            return HIGH;
        }
        return CRITICAL;
    }

    public RiskLevel max(RiskLevel other) {
        return this.rank >= other.rank ? this : other;
    }
}

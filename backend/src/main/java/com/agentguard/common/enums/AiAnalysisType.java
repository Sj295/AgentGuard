package com.agentguard.common.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum AiAnalysisType {
    GIT_DIFF_ANALYSIS,
    RISK_EXPLAIN,
    REPORT_SUMMARY;

    private static final String ALL_VALUES = Arrays.stream(values())
            .map(Enum::name)
            .collect(Collectors.joining(", "));

    public static AiAnalysisType fromCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AI analysis type cannot be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (AiAnalysisType analysisType : values()) {
            if (analysisType.name().equals(normalized)) {
                return analysisType;
            }
        }
        throw new IllegalArgumentException("Unsupported analysisType, expected one of: " + ALL_VALUES);
    }
}

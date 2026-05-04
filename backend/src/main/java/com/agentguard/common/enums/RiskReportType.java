package com.agentguard.common.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum RiskReportType {
    PERMISSION_ASSESS,
    GIT_DIFF_AUDIT,
    SENSITIVE_FILE_SCAN,
    COMMAND_AUDIT,
    PREFLIGHT_CHECK,
    MARKDOWN_REPORT,
    PROJECT_SCAN;

    private static final String ALL_VALUES = Arrays.stream(values())
            .map(Enum::name)
            .collect(Collectors.joining(", "));

    public static RiskReportType fromCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Report type cannot be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (RiskReportType reportType : values()) {
            if (reportType.name().equals(normalized)) {
                return reportType;
            }
        }
        throw new IllegalArgumentException("Unsupported reportType, expected one of: " + ALL_VALUES);
    }
}

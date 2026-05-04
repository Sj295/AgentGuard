package com.agentguard.common.enums;

import java.util.Locale;

public enum TaskType {
    READ_ONLY_ANALYSIS,
    BUG_FIX,
    FRONTEND_REFACTOR,
    NEW_FEATURE,
    TEST_WRITING,
    DOCUMENTATION,
    DEPENDENCY_INSTALL,
    LARGE_REFACTOR,
    GIT_OPERATION;

    public static TaskType fromCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Task type cannot be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (TaskType taskType : values()) {
            if (taskType.name().equals(normalized)) {
                return taskType;
            }
        }
        throw new IllegalArgumentException("Unsupported taskType, expected one of: READ_ONLY_ANALYSIS, BUG_FIX, FRONTEND_REFACTOR, NEW_FEATURE, TEST_WRITING, DOCUMENTATION, DEPENDENCY_INSTALL, LARGE_REFACTOR, GIT_OPERATION");
    }
}

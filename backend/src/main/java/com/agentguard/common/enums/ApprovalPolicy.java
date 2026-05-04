package com.agentguard.common.enums;

import java.util.Locale;

public enum ApprovalPolicy {
    ALWAYS,
    ON_REQUEST,
    ON_FAILURE,
    AUTO_APPROVE,
    NEVER;

    public static ApprovalPolicy fromCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Approval policy cannot be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (ApprovalPolicy policy : values()) {
            if (policy.name().equals(normalized)) {
                return policy;
            }
        }
        throw new IllegalArgumentException("Unsupported approvalPolicy, expected one of: ALWAYS, ON_REQUEST, ON_FAILURE, AUTO_APPROVE, NEVER");
    }
}

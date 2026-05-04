package com.agentguard.common.enums;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public enum AgentType {
    CODEX("CODEX", "AGENTS.md", new String[]{"AGENTS.md"}),
    CLAUDE("CLAUDE", "CLAUDE.md", new String[]{"CLAUDE.md"}),
    CURSOR("CURSOR", ".cursor/rules/agentguard.mdc", new String[]{".cursor", "rules", "agentguard.mdc"});

    private final String code;
    private final String fileName;
    private final String[] relativePathSegments;

    AgentType(String code, String fileName, String[] relativePathSegments) {
        this.code = code;
        this.fileName = fileName;
        this.relativePathSegments = relativePathSegments;
    }

    public String getCode() {
        return code;
    }

    public String getFileName() {
        return fileName;
    }

    public static AgentType fromCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Agent type cannot be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (AgentType agentType : values()) {
            if (agentType.code.equals(normalized)) {
                return agentType;
            }
        }
        throw new IllegalArgumentException("Unsupported agentType, expected one of: CODEX, CLAUDE, CURSOR");
    }

    public String resolveSuggestedPath(String projectPath) {
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException("Project path cannot be blank");
        }
        Path basePath = Paths.get(projectPath);
        Path resolved = basePath;
        for (String segment : relativePathSegments) {
            resolved = resolved.resolve(segment);
        }
        return resolved.normalize().toString().replace('\\', '/');
    }
}

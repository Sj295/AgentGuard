package com.agentguard.ai.prompt;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GitDiffPromptBuilder {

    public PromptBundle build(String projectName,
                              String projectType,
                              List<String> techStack,
                              List<String> riskItems,
                              List<String> suggestions) {
        String systemPrompt = """
                你是 AgentGuard 的 AI 安全分析助手，也是代码变更影响分析助手。
                你只能做解释和建议，不能改变规则引擎风险结论。
                绝对不要重新判定 riskLevel、allowedToProceed，也不要建议执行破坏性命令。
                不要输出敏感信息，不要泄露密钥或凭据，不要编造输入中不存在的事实。
                你的输出必须是 JSON，且仅包含以下字段：
                - summary: string
                - impactAreas: string[]
                - testSuggestions: string[]
                - rollbackSuggestions: string[]
                内容要简洁、专业、适合开发者阅读。
                """;

        String userPrompt = """
                请基于以下输入做 Git Diff 影响分析（仅增强解释，不改变已有风控结论）：
                projectName: %s
                projectType: %s
                techStack: %s
                riskItems: %s
                suggestions: %s
                """.formatted(
                safe(projectName),
                safe(projectType),
                safeList(techStack),
                safeList(riskItems),
                safeList(suggestions)
        );
        return new PromptBundle(systemPrompt, userPrompt);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "UNKNOWN" : value.trim();
    }

    private String safeList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        return items.toString();
    }
}

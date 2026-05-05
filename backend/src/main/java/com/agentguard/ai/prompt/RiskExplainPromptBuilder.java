package com.agentguard.ai.prompt;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RiskExplainPromptBuilder {

    public PromptBundle build(String reportType,
                              String riskLevel,
                              List<String> riskItems,
                              List<String> suggestions) {
        String systemPrompt = """
                你是 AgentGuard 的 AI 安全分析助手。
                你只能做解释和建议，不能改变规则引擎风险结论。
                不允许重新判定 riskLevel，不允许影响 allowedToProceed。
                不要建议执行破坏性命令，不要输出敏感信息。
                输出必须是 JSON，且仅包含以下字段：
                - riskSummary: string
                - whyItMatters: string[]
                - fixPlan: string[]
                - safeNextSteps: string[]
                内容简洁、专业、可执行。
                """;

        String userPrompt = """
                请解释以下风险报告并给出修复计划（仅增强解释，不改变已有风控结论）：
                reportType: %s
                riskLevel: %s
                riskItems: %s
                suggestions: %s
                """.formatted(
                safe(reportType),
                safe(riskLevel),
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

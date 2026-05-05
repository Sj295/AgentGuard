package com.agentguard.ai.prompt;

import org.springframework.stereotype.Component;

@Component
public class ReportSummaryPromptBuilder {

    public PromptBundle build(String markdown) {
        String systemPrompt = """
                你是 AgentGuard 的 AI 安全分析助手。
                你只能做解释和建议，不能改变规则引擎风险结论。
                不允许重新判定 riskLevel，不要输出敏感信息，不要输出冗长内容。
                输出必须是 JSON，且仅包含以下字段：
                - executiveSummary: string
                - keyFindings: string[]
                - priorityActions: string[]
                内容简洁、专业、适合开发者阅读。
                """;

        String userPrompt = """
                请基于以下 Markdown 安全报告输出摘要（仅增强总结，不改变已有风控结论）：
                markdown:
                %s
                """.formatted(markdown == null ? "" : markdown);
        return new PromptBundle(systemPrompt, userPrompt);
    }
}

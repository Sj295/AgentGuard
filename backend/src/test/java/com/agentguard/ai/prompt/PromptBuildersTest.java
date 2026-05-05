package com.agentguard.ai.prompt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuildersTest {

    private final GitDiffPromptBuilder gitDiffBuilder = new GitDiffPromptBuilder();
    private final RiskExplainPromptBuilder riskExplainBuilder = new RiskExplainPromptBuilder();
    private final ReportSummaryPromptBuilder reportSummaryBuilder = new ReportSummaryPromptBuilder();

    // ========== GitDiffPromptBuilder ==========

    @Test
    void gitDiff_shouldBuildPromptWithAllFields() {
        PromptBundle bundle = gitDiffBuilder.build(
                "MyProject", "JAVA_MAVEN",
                List.of("Java", "Spring Boot"),
                List.of("高危命令", "权限过高"),
                List.of("运行测试", "检查配置")
        );

        assertThat(bundle.systemPrompt()).contains("AgentGuard");
        assertThat(bundle.systemPrompt()).contains("JSON");
        assertThat(bundle.systemPrompt()).contains("summary");
        assertThat(bundle.systemPrompt()).contains("impactAreas");
        assertThat(bundle.systemPrompt()).contains("testSuggestions");
        assertThat(bundle.systemPrompt()).contains("rollbackSuggestions");

        assertThat(bundle.userPrompt()).contains("MyProject");
        assertThat(bundle.userPrompt()).contains("JAVA_MAVEN");
        assertThat(bundle.userPrompt()).contains("Java");
        assertThat(bundle.userPrompt()).contains("高危命令");
        assertThat(bundle.userPrompt()).contains("运行测试");
    }

    @Test
    void gitDiff_shouldHandleNullValues() {
        PromptBundle bundle = gitDiffBuilder.build(null, null, null, null, null);

        assertThat(bundle.systemPrompt()).isNotBlank();
        assertThat(bundle.userPrompt()).contains("UNKNOWN");
        assertThat(bundle.userPrompt()).contains("[]");
    }

    @Test
    void gitDiff_shouldHandleEmptyLists() {
        PromptBundle bundle = gitDiffBuilder.build("Project", "TYPE", List.of(), List.of(), List.of());

        assertThat(bundle.userPrompt()).contains("[]");
    }

    @Test
    void gitDiff_systemPromptShouldContainSafetyConstraints() {
        PromptBundle bundle = gitDiffBuilder.build("P", "T", List.of(), List.of(), List.of());

        assertThat(bundle.systemPrompt()).containsIgnoringCase("不要");
        assertThat(bundle.systemPrompt()).contains("riskLevel");
    }

    // ========== RiskExplainPromptBuilder ==========

    @Test
    void riskExplain_shouldBuildPromptWithAllFields() {
        PromptBundle bundle = riskExplainBuilder.build(
                "COMMAND_AUDIT", "CRITICAL",
                List.of("rm -rf 命令"),
                List.of("移除危险命令")
        );

        assertThat(bundle.systemPrompt()).contains("JSON");
        assertThat(bundle.systemPrompt()).contains("riskSummary");
        assertThat(bundle.systemPrompt()).contains("whyItMatters");
        assertThat(bundle.systemPrompt()).contains("fixPlan");
        assertThat(bundle.systemPrompt()).contains("safeNextSteps");

        assertThat(bundle.userPrompt()).contains("COMMAND_AUDIT");
        assertThat(bundle.userPrompt()).contains("CRITICAL");
        assertThat(bundle.userPrompt()).contains("rm -rf 命令");
        assertThat(bundle.userPrompt()).contains("移除危险命令");
    }

    @Test
    void riskExplain_shouldHandleNullValues() {
        PromptBundle bundle = riskExplainBuilder.build(null, null, null, null);

        assertThat(bundle.systemPrompt()).isNotBlank();
        assertThat(bundle.userPrompt()).contains("UNKNOWN");
        assertThat(bundle.userPrompt()).contains("[]");
    }

    @Test
    void riskExplain_systemPromptShouldContainSafetyConstraints() {
        PromptBundle bundle = riskExplainBuilder.build("T", "L", List.of(), List.of());

        assertThat(bundle.systemPrompt()).contains("不允许重新判定 riskLevel");
        assertThat(bundle.systemPrompt()).contains("不允许影响 allowedToProceed");
    }

    // ========== ReportSummaryPromptBuilder ==========

    @Test
    void reportSummary_shouldBuildPromptWithMarkdown() {
        PromptBundle bundle = reportSummaryBuilder.build("# Security Report\n- Risk: HIGH");

        assertThat(bundle.systemPrompt()).contains("JSON");
        assertThat(bundle.systemPrompt()).contains("executiveSummary");
        assertThat(bundle.systemPrompt()).contains("keyFindings");
        assertThat(bundle.systemPrompt()).contains("priorityActions");

        assertThat(bundle.userPrompt()).contains("# Security Report");
        assertThat(bundle.userPrompt()).contains("Risk: HIGH");
    }

    @Test
    void reportSummary_shouldHandleNullMarkdown() {
        PromptBundle bundle = reportSummaryBuilder.build(null);

        assertThat(bundle.systemPrompt()).isNotBlank();
        assertThat(bundle.userPrompt()).isNotBlank();
    }

    @Test
    void reportSummary_shouldHandleEmptyMarkdown() {
        PromptBundle bundle = reportSummaryBuilder.build("");

        assertThat(bundle.systemPrompt()).isNotBlank();
        assertThat(bundle.userPrompt()).isNotBlank();
    }

    @Test
    void reportSummary_systemPromptShouldContainSafetyConstraints() {
        PromptBundle bundle = reportSummaryBuilder.build("content");

        assertThat(bundle.systemPrompt()).contains("不允许重新判定 riskLevel");
        assertThat(bundle.systemPrompt()).contains("不要输出敏感信息");
    }

    // ========== PromptBundle record ==========

    @Test
    void promptBundle_shouldBeRecord() {
        PromptBundle bundle = new PromptBundle("system", "user");
        assertThat(bundle.systemPrompt()).isEqualTo("system");
        assertThat(bundle.userPrompt()).isEqualTo("user");
    }

    @Test
    void promptBundle_shouldSupportEquality() {
        PromptBundle a = new PromptBundle("s", "u");
        PromptBundle b = new PromptBundle("s", "u");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}

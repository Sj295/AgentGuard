package com.agentguard.ai.service.impl;

import com.agentguard.ai.cache.AiAnalysisCacheService;
import com.agentguard.ai.cache.AiRateLimitService;
import com.agentguard.ai.client.LlmClient;
import com.agentguard.ai.config.AiProperties;
import com.agentguard.ai.dto.AiGitDiffAnalysisRequest;
import com.agentguard.ai.dto.AiReportSummaryRequest;
import com.agentguard.ai.dto.AiRiskExplainRequest;
import com.agentguard.ai.prompt.GitDiffPromptBuilder;
import com.agentguard.ai.prompt.ReportSummaryPromptBuilder;
import com.agentguard.ai.prompt.RiskExplainPromptBuilder;
import com.agentguard.ai.service.AiAnalysisService;
import com.agentguard.ai.vo.AiGitDiffAnalysisVO;
import com.agentguard.ai.vo.AiReportSummaryVO;
import com.agentguard.ai.vo.AiRiskExplainVO;
import com.agentguard.common.BusinessException;
import com.agentguard.entity.AiAnalysisRecord;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.RiskReport;
import com.agentguard.service.AiAnalysisRecordService;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.RiskReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LlmAiAnalysisServiceImplTest {

    private LlmClient llmClient;
    private AiProperties aiProperties;
    private ProjectInfoService projectInfoService;
    private RiskReportService riskReportService;
    private AiAnalysisRecordService aiAnalysisRecordService;
    private MockAiAnalysisServiceImpl mockAiAnalysisService;
    private LlmAiAnalysisServiceImpl service;

    private static final Long PROJECT_ID = 1L;
    private static final Long REPORT_ID = 100L;

    @BeforeEach
    void setUp() {
        llmClient = Mockito.mock(LlmClient.class);
        aiProperties = new AiProperties();
        aiProperties.setEnabled(true);
        aiProperties.setApiKey("test-key");
        aiProperties.setProvider("test-provider");
        aiProperties.setModel("test-model");
        projectInfoService = Mockito.mock(ProjectInfoService.class);
        riskReportService = Mockito.mock(RiskReportService.class);
        aiAnalysisRecordService = Mockito.mock(AiAnalysisRecordService.class);
        mockAiAnalysisService = Mockito.mock(MockAiAnalysisServiceImpl.class);
        ObjectMapper objectMapper = new ObjectMapper();

        service = new LlmAiAnalysisServiceImpl(
                projectInfoService,
                riskReportService,
                aiAnalysisRecordService,
                llmClient,
                aiProperties,
                new GitDiffPromptBuilder(),
                new RiskExplainPromptBuilder(),
                new ReportSummaryPromptBuilder(),
                mockAiAnalysisService,
                objectMapper
        );
    }

    // ========== JSON Parsing Tests ==========

    @Test
    void analyzeGitDiff_shouldParseDirectJson() {
        setupProjectAndReport("GIT_DIFF_AUDIT");
        String jsonResponse = """
                {"summary":"变更影响较小","impactAreas":["配置文件"],"testSuggestions":["运行单元测试"],"rollbackSuggestions":["git restore"]}
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(jsonResponse);

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(buildGitDiffRequest());

        assertThat(vo.getSummary()).isEqualTo("变更影响较小");
        assertThat(vo.getImpactAreas()).containsExactly("配置文件");
        assertThat(vo.getTestSuggestions()).containsExactly("运行单元测试");
        assertThat(vo.getRollbackSuggestions()).containsExactly("git restore");
        assertThat(vo.getConfidenceNote()).contains("仅供参考");
        assertThat(vo.isMocked()).isFalse();
        assertThat(vo.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(vo.getGitAuditReportId()).isEqualTo(REPORT_ID);
    }

    @Test
    void analyzeGitDiff_shouldParseJsonWrappedInCodeBlock() {
        setupProjectAndReport("GIT_DIFF_AUDIT");
        String response = """
                ```json
                {"summary":"代码块中的JSON","impactAreas":["前端模块"],"testSuggestions":["npm test"],"rollbackSuggestions":["回滚"]}
                ```
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(response);

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(buildGitDiffRequest());

        assertThat(vo.getSummary()).isEqualTo("代码块中的JSON");
        assertThat(vo.getImpactAreas()).containsExactly("前端模块");
    }

    @Test
    void analyzeGitDiff_shouldParseJsonFromBraceExtraction() {
        setupProjectAndReport("GIT_DIFF_AUDIT");
        String response = "Here is my analysis:\n{\"summary\":\"提取的JSON\",\"impactAreas\":[\"后端\"],\"testSuggestions\":[\"mvn test\"],\"rollbackSuggestions\":[\"revert\"]}\nHope this helps.";
        when(llmClient.chat(anyString(), anyString())).thenReturn(response);

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(buildGitDiffRequest());

        assertThat(vo.getSummary()).isEqualTo("提取的JSON");
        assertThat(vo.getImpactAreas()).containsExactly("后端");
    }

    @Test
    void analyzeGitDiff_shouldFallbackWhenJsonParsingFails() {
        setupProjectAndReport("GIT_DIFF_AUDIT");
        when(llmClient.chat(anyString(), anyString())).thenReturn("This is not JSON at all, just plain text analysis.");

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(buildGitDiffRequest());

        assertThat(vo.getSummary()).isNotEmpty();
        assertThat(vo.getImpactAreas()).isNotEmpty();
        assertThat(vo.getTestSuggestions()).isNotEmpty();
        assertThat(vo.getRollbackSuggestions()).isNotEmpty();
        assertThat(vo.getConfidenceNote()).contains("仅供参考");
    }

    @Test
    void analyzeGitDiff_shouldFallbackWhenResponseIsEmpty() {
        setupProjectAndReport("GIT_DIFF_AUDIT");
        when(llmClient.chat(anyString(), anyString())).thenReturn("");

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(buildGitDiffRequest());

        assertThat(vo.getSummary()).isNotEmpty();
        assertThat(vo.getImpactAreas()).isNotEmpty();
    }

    @Test
    void analyzeGitDiff_shouldFallbackWhenResponseIsNull() {
        setupProjectAndReport("GIT_DIFF_AUDIT");
        when(llmClient.chat(anyString(), anyString())).thenReturn(null);

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(buildGitDiffRequest());

        assertThat(vo.getSummary()).isNotEmpty();
    }

    @Test
    void analyzeGitDiff_shouldReturnCacheHitBeforeRateLimitAndLlmCall() {
        AiAnalysisCacheService cacheService = Mockito.mock(AiAnalysisCacheService.class);
        AiRateLimitService rateLimitService = Mockito.mock(AiRateLimitService.class);
        AiGitDiffAnalysisVO cached = new AiGitDiffAnalysisVO();
        cached.setSummary("cached result");
        cached.setImpactAreas(List.of("cached area"));
        cached.setTestSuggestions(List.of("cached test"));
        cached.setRollbackSuggestions(List.of("cached rollback"));
        cached.setMocked(false);
        cached.setCached(true);

        when(cacheService.getGitDiffAnalysis(REPORT_ID)).thenReturn(Optional.of(cached));
        ReflectionTestUtils.setField(service, "aiAnalysisCacheService", cacheService);
        ReflectionTestUtils.setField(service, "aiRateLimitService", rateLimitService);

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(buildGitDiffRequest());

        assertThat(vo.getCached()).isTrue();
        assertThat(vo.getSummary()).isEqualTo("cached result");
        assertThat(vo.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(vo.getGitAuditReportId()).isEqualTo(REPORT_ID);
        verify(cacheService).getGitDiffAnalysis(REPORT_ID);
        verifyNoInteractions(rateLimitService, projectInfoService, riskReportService, llmClient);
        verify(aiAnalysisRecordService).saveRecordSafely(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getProvider()).isEqualTo("cache");
        assertThat(recordCaptor.getValue().getInputSummary()).contains("cache hit");
    }

    // ========== Risk Explain JSON Parsing ==========

    @Test
    void explainRisk_shouldParseDirectJson() {
        setupProjectAndReport("COMMAND_AUDIT");
        String jsonResponse = """
                {"riskSummary":"高危命令风险","whyItMatters":["可能导致数据丢失"],"fixPlan":["移除rm命令"],"safeNextSteps":["检查工作区"]}
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(jsonResponse);

        AiRiskExplainVO vo = service.explainRisk(buildRiskExplainRequest());

        assertThat(vo.getRiskSummary()).isEqualTo("高危命令风险");
        assertThat(vo.getWhyItMatters()).containsExactly("可能导致数据丢失");
        assertThat(vo.getFixPlan()).containsExactly("移除rm命令");
        assertThat(vo.getSafeNextSteps()).containsExactly("检查工作区");
        assertThat(vo.getConfidenceNote()).contains("仅供参考");
        assertThat(vo.isMocked()).isFalse();
    }

    @Test
    void explainRisk_shouldParseJsonWrappedInCodeBlock() {
        setupProjectAndReport("PERMISSION_ASSESS");
        String response = """
                ```json
                {"riskSummary":"权限过高","whyItMatters":["越权风险"],"fixPlan":["收紧权限"],"safeNextSteps":["重新评估"]}
                ```
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(response);

        AiRiskExplainVO vo = service.explainRisk(buildRiskExplainRequest());

        assertThat(vo.getRiskSummary()).isEqualTo("权限过高");
    }

    @Test
    void explainRisk_shouldFallbackWhenNonJson() {
        setupProjectAndReport("COMMAND_AUDIT");
        when(llmClient.chat(anyString(), anyString())).thenReturn("Plain text explanation without JSON structure.");

        AiRiskExplainVO vo = service.explainRisk(buildRiskExplainRequest());

        assertThat(vo.getRiskSummary()).isNotEmpty();
        assertThat(vo.getWhyItMatters()).isNotEmpty();
        assertThat(vo.getFixPlan()).isNotEmpty();
        assertThat(vo.getSafeNextSteps()).isNotEmpty();
    }

    // ========== Report Summary JSON Parsing ==========

    @Test
    void summarizeReport_shouldParseDirectJson() {
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(buildProjectInfo());
        String jsonResponse = """
                {"executiveSummary":"项目安全状况良好","keyFindings":["无高危漏洞"],"priorityActions":["持续监控"]}
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(jsonResponse);

        AiReportSummaryVO vo = service.summarizeReport(buildReportSummaryRequest());

        assertThat(vo.getExecutiveSummary()).isEqualTo("项目安全状况良好");
        assertThat(vo.getKeyFindings()).containsExactly("无高危漏洞");
        assertThat(vo.getPriorityActions()).containsExactly("持续监控");
        assertThat(vo.getConfidenceNote()).contains("仅供参考");
        assertThat(vo.isMocked()).isFalse();
    }

    @Test
    void summarizeReport_shouldParseJsonWrappedInCodeBlock() {
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(buildProjectInfo());
        String response = """
                ```json
                {"executiveSummary":"摘要","keyFindings":["发现1"],"priorityActions":["行动1"]}
                ```
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(response);

        AiReportSummaryVO vo = service.summarizeReport(buildReportSummaryRequest());

        assertThat(vo.getExecutiveSummary()).isEqualTo("摘要");
    }

    @Test
    void summarizeReport_shouldFallbackWhenNonJson() {
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(buildProjectInfo());
        when(llmClient.chat(anyString(), anyString())).thenReturn("This is a plain text summary without JSON.");

        AiReportSummaryVO vo = service.summarizeReport(buildReportSummaryRequest());

        assertThat(vo.getExecutiveSummary()).isNotEmpty();
        assertThat(vo.getKeyFindings()).isNotEmpty();
        assertThat(vo.getPriorityActions()).isNotEmpty();
    }

    // ========== Degradation to Mock ==========

    @Test
    void analyzeGitDiff_shouldFallbackToMockOnLlmException() {
        setupProjectAndReport("GIT_DIFF_AUDIT");
        when(llmClient.chat(anyString(), anyString())).thenThrow(new RuntimeException("Connection timeout"));
        AiGitDiffAnalysisVO mockVo = buildMockGitDiffVO();
        when(mockAiAnalysisService.buildGitDiffMock(any())).thenReturn(mockVo);

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(buildGitDiffRequest());

        assertThat(vo.isMocked()).isTrue();
        assertThat(vo.getSummary()).isEqualTo("mock summary");
        verify(aiAnalysisRecordService).saveRecordSafely(recordCaptor.capture());
        AiAnalysisRecord record = recordCaptor.getValue();
        assertThat(record.getMocked()).isTrue();
        assertThat(record.getSuccess()).isFalse();
        assertThat(record.getErrorMessage()).contains("Connection timeout");
        assertThat(record.getProvider()).isEqualTo("fallback-mock");
    }

    @Test
    void explainRisk_shouldFallbackToMockOnLlmException() {
        setupProjectAndReport("COMMAND_AUDIT");
        when(llmClient.chat(anyString(), anyString())).thenThrow(new RuntimeException("API rate limited"));
        AiRiskExplainVO mockVo = buildMockRiskExplainVO();
        when(mockAiAnalysisService.buildRiskExplainMock(any())).thenReturn(mockVo);

        AiRiskExplainVO vo = service.explainRisk(buildRiskExplainRequest());

        assertThat(vo.isMocked()).isTrue();
        verify(aiAnalysisRecordService).saveRecordSafely(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getSuccess()).isFalse();
        assertThat(recordCaptor.getValue().getErrorMessage()).contains("API rate limited");
    }

    @Test
    void summarizeReport_shouldFallbackToMockOnLlmException() {
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(buildProjectInfo());
        when(llmClient.chat(anyString(), anyString())).thenThrow(new IllegalStateException("Failed to call Spring AI provider"));
        AiReportSummaryVO mockVo = buildMockReportSummaryVO();
        when(mockAiAnalysisService.buildReportSummaryMock(any())).thenReturn(mockVo);

        AiReportSummaryVO vo = service.summarizeReport(buildReportSummaryRequest());

        assertThat(vo.isMocked()).isTrue();
        verify(aiAnalysisRecordService).saveRecordSafely(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getSuccess()).isFalse();
        assertThat(recordCaptor.getValue().getErrorMessage()).contains("Failed to call Spring AI provider");
    }

    // ========== Record Persistence on Success ==========

    @Test
    void analyzeGitDiff_shouldPersistRecordOnSuccess() {
        setupProjectAndReport("GIT_DIFF_AUDIT");
        when(llmClient.chat(anyString(), anyString())).thenReturn("{\"summary\":\"ok\",\"impactAreas\":[\"a\"],\"testSuggestions\":[\"b\"],\"rollbackSuggestions\":[\"c\"]}");

        service.analyzeGitDiff(buildGitDiffRequest());

        verify(aiAnalysisRecordService).saveRecordSafely(recordCaptor.capture());
        AiAnalysisRecord record = recordCaptor.getValue();
        assertThat(record.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(record.getAnalysisType()).isEqualTo("GIT_DIFF_ANALYSIS");
        assertThat(record.getSourceReportId()).isEqualTo(REPORT_ID);
        assertThat(record.getProvider()).isEqualTo("test-provider");
        assertThat(record.getModel()).isEqualTo("test-model");
        assertThat(record.getMocked()).isFalse();
        assertThat(record.getSuccess()).isTrue();
        assertThat(record.getLatencyMs()).isGreaterThanOrEqualTo(0);
        assertThat(record.getInputSummary()).contains("projectName=");
        assertThat(record.getOutputContent()).contains("summary");
        assertThat(record.getErrorMessage()).isNull();
    }

    @Test
    void explainRisk_shouldPersistRecordOnSuccess() {
        setupProjectAndReport("COMMAND_AUDIT");
        when(llmClient.chat(anyString(), anyString())).thenReturn("{\"riskSummary\":\"ok\",\"whyItMatters\":[\"a\"],\"fixPlan\":[\"b\"],\"safeNextSteps\":[\"c\"]}");

        service.explainRisk(buildRiskExplainRequest());

        verify(aiAnalysisRecordService).saveRecordSafely(recordCaptor.capture());
        AiAnalysisRecord record = recordCaptor.getValue();
        assertThat(record.getAnalysisType()).isEqualTo("RISK_EXPLAIN");
        assertThat(record.getSourceReportId()).isEqualTo(REPORT_ID);
        assertThat(record.getSuccess()).isTrue();
        assertThat(record.getMocked()).isFalse();
    }

    @Test
    void summarizeReport_shouldPersistRecordOnSuccess() {
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(buildProjectInfo());
        when(llmClient.chat(anyString(), anyString())).thenReturn("{\"executiveSummary\":\"ok\",\"keyFindings\":[\"a\"],\"priorityActions\":[\"b\"]}");

        service.summarizeReport(buildReportSummaryRequest());

        verify(aiAnalysisRecordService).saveRecordSafely(recordCaptor.capture());
        AiAnalysisRecord record = recordCaptor.getValue();
        assertThat(record.getAnalysisType()).isEqualTo("REPORT_SUMMARY");
        assertThat(record.getSourceReportId()).isNull();
        assertThat(record.getSuccess()).isTrue();
    }

    // ========== Validation Tests ==========

    @Test
    void analyzeGitDiff_shouldThrowWhenProjectNotFound() {
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.analyzeGitDiff(buildGitDiffRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("项目不存在");
    }

    @Test
    void analyzeGitDiff_shouldThrowWhenReportNotFound() {
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(buildProjectInfo());
        when(riskReportService.getById(REPORT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.analyzeGitDiff(buildGitDiffRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("报告不存在");
    }

    @Test
    void analyzeGitDiff_shouldThrowWhenReportTypeMismatch() {
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(buildProjectInfo());
        RiskReport report = buildRiskReport("COMMAND_AUDIT");
        when(riskReportService.getById(REPORT_ID)).thenReturn(report);

        assertThatThrownBy(() -> service.analyzeGitDiff(buildGitDiffRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("GIT_DIFF_AUDIT");
    }

    @Test
    void analyzeGitDiff_shouldThrowWhenReportBelongsToDifferentProject() {
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(buildProjectInfo());
        RiskReport report = buildRiskReport("GIT_DIFF_AUDIT");
        report.setProjectId(999L);
        when(riskReportService.getById(REPORT_ID)).thenReturn(report);

        assertThatThrownBy(() -> service.analyzeGitDiff(buildGitDiffRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void explainRisk_shouldThrowWhenReportTypeNotExplainable() {
        setupProjectAndReport("SCAN_TASK");

        assertThatThrownBy(() -> service.explainRisk(buildRiskExplainRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PERMISSION_ASSESS, COMMAND_AUDIT, PREFLIGHT_CHECK, GIT_DIFF_AUDIT");
    }

    @Test
    void explainRisk_shouldAcceptAllExplainableTypes() {
        for (String type : List.of("PERMISSION_ASSESS", "COMMAND_AUDIT", "PREFLIGHT_CHECK", "GIT_DIFF_AUDIT")) {
            setupProjectAndReport(type);
            when(llmClient.chat(anyString(), anyString())).thenReturn("{\"riskSummary\":\"ok\",\"whyItMatters\":[\"a\"],\"fixPlan\":[\"b\"],\"safeNextSteps\":[\"c\"]}");

            AiRiskExplainVO vo = service.explainRisk(buildRiskExplainRequest());
            assertThat(vo).isNotNull();
        }
    }

    // ========== Sanitization Tests ==========

    @Test
    void analyzeGitDiff_shouldSanitizeDangerousCommands() {
        setupProjectAndReport("GIT_DIFF_AUDIT");
        String response = """
                {"summary":"分析完成","impactAreas":["模块A"],"testSuggestions":["rm -rf /","正常测试命令"],"rollbackSuggestions":["git reset --hard","正常回滚"]}
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(response);

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(buildGitDiffRequest());

        assertThat(vo.getTestSuggestions()).doesNotContain("rm -rf /");
        assertThat(vo.getTestSuggestions()).contains("正常测试命令");
        assertThat(vo.getRollbackSuggestions()).doesNotContain("git reset --hard");
        assertThat(vo.getRollbackSuggestions()).contains("正常回滚");
    }

    @Test
    void summarizeReport_shouldTruncateLongMarkdown() {
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(buildProjectInfo());
        String longMarkdown = "# Report\n" + "A".repeat(20000);
        when(llmClient.chat(anyString(), anyString())).thenReturn("{\"executiveSummary\":\"ok\",\"keyFindings\":[\"a\"],\"priorityActions\":[\"b\"]}");

        AiReportSummaryRequest longReq = new AiReportSummaryRequest();
        longReq.setProjectId(PROJECT_ID);
        longReq.setMarkdown(longMarkdown);
        service.summarizeReport(longReq);

        verify(llmClient).chat(anyString(), userPromptCaptor.capture());
        String userPrompt = userPromptCaptor.getValue();
        // The markdown in the prompt should be truncated to 12000 chars
        assertThat(userPrompt.length()).isLessThan(longMarkdown.length());
    }

    @Test
    void summarizeReport_shouldHandleNullMarkdown() {
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(buildProjectInfo());
        when(llmClient.chat(anyString(), anyString())).thenReturn("{\"executiveSummary\":\"ok\",\"keyFindings\":[\"a\"],\"priorityActions\":[\"b\"]}");

        AiReportSummaryRequest request = new AiReportSummaryRequest();
        request.setProjectId(PROJECT_ID);
        request.setMarkdown(null);

        // Should not throw, markdown is handled gracefully
        AiReportSummaryVO vo = service.summarizeReport(request);
        assertThat(vo).isNotNull();
    }

    // ========== JSON with extra fields ==========

    @Test
    void analyzeGitDiff_shouldIgnoreExtraJsonFields() {
        setupProjectAndReport("GIT_DIFF_AUDIT");
        String response = """
                {"summary":"ok","impactAreas":["a"],"testSuggestions":["b"],"rollbackSuggestions":["c"],"extraField":"ignored","another":123}
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(response);

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(buildGitDiffRequest());

        assertThat(vo.getSummary()).isEqualTo("ok");
    }

    @Test
    void analyzeGitDiff_shouldUseFallbackForMissingJsonFields() {
        setupProjectAndReport("GIT_DIFF_AUDIT");
        String response = """
                {"summary":"partial data"}
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(response);

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(buildGitDiffRequest());

        assertThat(vo.getSummary()).isEqualTo("partial data");
        assertThat(vo.getImpactAreas()).isNotEmpty();
        assertThat(vo.getTestSuggestions()).isNotEmpty();
        assertThat(vo.getRollbackSuggestions()).isNotEmpty();
    }

    @Test
    void analyzeGitDiff_shouldHandleJsonWithEmptyArrays() {
        setupProjectAndReport("GIT_DIFF_AUDIT");
        String response = """
                {"summary":"ok","impactAreas":[],"testSuggestions":[],"rollbackSuggestions":[]}
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(response);

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(buildGitDiffRequest());

        assertThat(vo.getSummary()).isEqualTo("ok");
        // Empty arrays should fall back to defaults
        assertThat(vo.getImpactAreas()).isNotEmpty();
        assertThat(vo.getTestSuggestions()).isNotEmpty();
        assertThat(vo.getRollbackSuggestions()).isNotEmpty();
    }

    // ========== Sensitive Data Sanitization in Input Summary ==========

    @Test
    void analyzeGitDiff_shouldSanitizeSensitiveDataInInputSummary() {
        ProjectInfo project = buildProjectInfo();
        project.setProjectName("api_key=sk-12345-secret");
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(project);
        RiskReport report = buildRiskReport("GIT_DIFF_AUDIT");
        report.setSummary("token=abc123secret");
        when(riskReportService.getById(REPORT_ID)).thenReturn(report);
        when(llmClient.chat(anyString(), anyString())).thenReturn("{\"summary\":\"ok\",\"impactAreas\":[\"a\"],\"testSuggestions\":[\"b\"],\"rollbackSuggestions\":[\"c\"]}");

        service.analyzeGitDiff(buildGitDiffRequest());

        verify(aiAnalysisRecordService).saveRecordSafely(recordCaptor.capture());
        String inputSummary = recordCaptor.getValue().getInputSummary();
        assertThat(inputSummary).doesNotContain("sk-12345-secret");
        assertThat(inputSummary).doesNotContain("abc123secret");
        assertThat(inputSummary).contains("***");
    }

    // ========== Report Summary with markdown containing sensitive data ==========

    @Test
    void summarizeReport_shouldSanitizeSensitiveDataInSnippet() {
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(buildProjectInfo());
        String markdownWithSecret = "# Report\napi_key=sk-super-secret-token\nNormal content here.";
        when(llmClient.chat(anyString(), anyString())).thenReturn("{\"executiveSummary\":\"ok\",\"keyFindings\":[\"a\"],\"priorityActions\":[\"b\"]}");

        AiReportSummaryRequest secretReq = new AiReportSummaryRequest();
        secretReq.setProjectId(PROJECT_ID);
        secretReq.setMarkdown(markdownWithSecret);
        service.summarizeReport(secretReq);

        verify(aiAnalysisRecordService).saveRecordSafely(recordCaptor.capture());
        String inputSummary = recordCaptor.getValue().getInputSummary();
        assertThat(inputSummary).doesNotContain("sk-super-secret-token");
    }

    // ========== Capture fields ==========

    private final ArgumentCaptor<AiAnalysisRecord> recordCaptor = ArgumentCaptor.forClass(AiAnalysisRecord.class);
    private final ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);

    // ========== Helper Methods ==========

    private void setupProjectAndReport(String reportType) {
        when(projectInfoService.getById(PROJECT_ID)).thenReturn(buildProjectInfo());
        when(riskReportService.getById(REPORT_ID)).thenReturn(buildRiskReport(reportType));
    }

    private ProjectInfo buildProjectInfo() {
        ProjectInfo info = new ProjectInfo();
        info.setId(PROJECT_ID);
        info.setProjectName("TestProject");
        info.setProjectType("JAVA_MAVEN");
        info.setTechStack("[\"Java\",\"Spring Boot\"]");
        return info;
    }

    private RiskReport buildRiskReport(String reportType) {
        RiskReport report = new RiskReport();
        report.setId(REPORT_ID);
        report.setProjectId(PROJECT_ID);
        report.setReportType(reportType);
        report.setRiskLevel("HIGH");
        report.setRiskScore(80);
        report.setSummary("Risk summary");
        report.setRiskItems("[\"风险项1\",\"风险项2\"]");
        report.setSuggestions("[\"建议1\",\"建议2\"]");
        report.setPayloadJson("{\"addedFiles\":[\"src/Main.java\"],\"modifiedFiles\":[\"pom.xml\"],\"deletedFiles\":[]}");
        return report;
    }

    private AiGitDiffAnalysisRequest buildGitDiffRequest() {
        AiGitDiffAnalysisRequest req = new AiGitDiffAnalysisRequest();
        req.setProjectId(PROJECT_ID);
        req.setGitAuditReportId(REPORT_ID);
        return req;
    }

    private AiRiskExplainRequest buildRiskExplainRequest() {
        AiRiskExplainRequest req = new AiRiskExplainRequest();
        req.setProjectId(PROJECT_ID);
        req.setReportId(REPORT_ID);
        return req;
    }

    private AiReportSummaryRequest buildReportSummaryRequest() {
        AiReportSummaryRequest req = new AiReportSummaryRequest();
        req.setProjectId(PROJECT_ID);
        req.setMarkdown("# Security Report\n- Risk: HIGH");
        return req;
    }

    private AiGitDiffAnalysisVO buildMockGitDiffVO() {
        AiGitDiffAnalysisVO vo = new AiGitDiffAnalysisVO();
        vo.setSummary("mock summary");
        vo.setImpactAreas(List.of("mock impact"));
        vo.setTestSuggestions(List.of("mock test"));
        vo.setRollbackSuggestions(List.of("mock rollback"));
        vo.setConfidenceNote(AiAnalysisService.CONFIDENCE_NOTE);
        vo.setMocked(true);
        return vo;
    }

    private AiRiskExplainVO buildMockRiskExplainVO() {
        AiRiskExplainVO vo = new AiRiskExplainVO();
        vo.setRiskSummary("mock risk summary");
        vo.setWhyItMatters(List.of("mock why"));
        vo.setFixPlan(List.of("mock fix"));
        vo.setSafeNextSteps(List.of("mock steps"));
        vo.setConfidenceNote(AiAnalysisService.CONFIDENCE_NOTE);
        vo.setMocked(true);
        return vo;
    }

    private AiReportSummaryVO buildMockReportSummaryVO() {
        AiReportSummaryVO vo = new AiReportSummaryVO();
        vo.setExecutiveSummary("mock executive summary");
        vo.setKeyFindings(List.of("mock finding"));
        vo.setPriorityActions(List.of("mock action"));
        vo.setConfidenceNote(AiAnalysisService.CONFIDENCE_NOTE);
        vo.setMocked(true);
        return vo;
    }
}

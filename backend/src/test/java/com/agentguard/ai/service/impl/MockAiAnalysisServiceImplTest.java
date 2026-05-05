package com.agentguard.ai.service.impl;

import com.agentguard.ai.dto.AiGitDiffAnalysisRequest;
import com.agentguard.ai.dto.AiReportSummaryRequest;
import com.agentguard.ai.dto.AiRiskExplainRequest;
import com.agentguard.ai.service.AiAnalysisService;
import com.agentguard.ai.vo.AiGitDiffAnalysisVO;
import com.agentguard.ai.vo.AiReportSummaryVO;
import com.agentguard.ai.vo.AiRiskExplainVO;
import com.agentguard.entity.AiAnalysisRecord;
import com.agentguard.service.AiAnalysisRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MockAiAnalysisServiceImplTest {

    private AiAnalysisRecordService aiAnalysisRecordService;
    private MockAiAnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        aiAnalysisRecordService = org.mockito.Mockito.mock(AiAnalysisRecordService.class);
        service = new MockAiAnalysisServiceImpl(aiAnalysisRecordService);
    }

    @Test
    void analyzeGitDiff_shouldReturnMockResultWithAllFields() {
        AiGitDiffAnalysisRequest request = new AiGitDiffAnalysisRequest();
        request.setProjectId(1L);
        request.setGitAuditReportId(100L);

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(request);

        assertThat(vo.getProjectId()).isEqualTo(1L);
        assertThat(vo.getGitAuditReportId()).isEqualTo(100L);
        assertThat(vo.getSummary()).isNotBlank();
        assertThat(vo.getImpactAreas()).hasSize(3);
        assertThat(vo.getTestSuggestions()).hasSize(3);
        assertThat(vo.getRollbackSuggestions()).hasSize(3);
        assertThat(vo.getConfidenceNote()).isEqualTo(AiAnalysisService.CONFIDENCE_NOTE);
        assertThat(vo.isMocked()).isTrue();
    }

    @Test
    void explainRisk_shouldReturnMockResultWithAllFields() {
        AiRiskExplainRequest request = new AiRiskExplainRequest();
        request.setProjectId(1L);
        request.setReportId(200L);

        AiRiskExplainVO vo = service.explainRisk(request);

        assertThat(vo.getProjectId()).isEqualTo(1L);
        assertThat(vo.getReportId()).isEqualTo(200L);
        assertThat(vo.getRiskSummary()).isNotBlank();
        assertThat(vo.getWhyItMatters()).hasSize(3);
        assertThat(vo.getFixPlan()).hasSize(3);
        assertThat(vo.getSafeNextSteps()).hasSize(3);
        assertThat(vo.getConfidenceNote()).isEqualTo(AiAnalysisService.CONFIDENCE_NOTE);
        assertThat(vo.isMocked()).isTrue();
    }

    @Test
    void summarizeReport_shouldReturnMockResultWithAllFields() {
        AiReportSummaryRequest request = new AiReportSummaryRequest();
        request.setProjectId(1L);
        request.setMarkdown("# Test Report");

        AiReportSummaryVO vo = service.summarizeReport(request);

        assertThat(vo.getProjectId()).isEqualTo(1L);
        assertThat(vo.getExecutiveSummary()).isNotBlank();
        assertThat(vo.getKeyFindings()).hasSize(3);
        assertThat(vo.getPriorityActions()).hasSize(3);
        assertThat(vo.getConfidenceNote()).isEqualTo(AiAnalysisService.CONFIDENCE_NOTE);
        assertThat(vo.isMocked()).isTrue();
    }

    @Test
    void analyzeGitDiff_shouldPersistRecordWithMockProvider() {
        AiGitDiffAnalysisRequest request = new AiGitDiffAnalysisRequest();
        request.setProjectId(1L);
        request.setGitAuditReportId(100L);

        service.analyzeGitDiff(request);

        ArgumentCaptor<AiAnalysisRecord> captor = ArgumentCaptor.forClass(AiAnalysisRecord.class);
        verify(aiAnalysisRecordService).saveRecordSafely(captor.capture());
        AiAnalysisRecord record = captor.getValue();
        assertThat(record.getProjectId()).isEqualTo(1L);
        assertThat(record.getAnalysisType()).isEqualTo("GIT_DIFF_ANALYSIS");
        assertThat(record.getSourceReportId()).isEqualTo(100L);
        assertThat(record.getProvider()).isEqualTo("mock");
        assertThat(record.getModel()).isEqualTo("mock-model");
        assertThat(record.getMocked()).isTrue();
        assertThat(record.getSuccess()).isTrue();
        assertThat(record.getLatencyMs()).isGreaterThanOrEqualTo(0);
        assertThat(record.getInputSummary()).contains("projectId=1");
        assertThat(record.getOutputContent()).isNotBlank();
        assertThat(record.getErrorMessage()).isNull();
    }

    @Test
    void explainRisk_shouldPersistRecordWithCorrectType() {
        AiRiskExplainRequest request = new AiRiskExplainRequest();
        request.setProjectId(2L);
        request.setReportId(200L);

        service.explainRisk(request);

        ArgumentCaptor<AiAnalysisRecord> captor = ArgumentCaptor.forClass(AiAnalysisRecord.class);
        verify(aiAnalysisRecordService).saveRecordSafely(captor.capture());
        AiAnalysisRecord record = captor.getValue();
        assertThat(record.getAnalysisType()).isEqualTo("RISK_EXPLAIN");
        assertThat(record.getSourceReportId()).isEqualTo(200L);
        assertThat(record.getProjectId()).isEqualTo(2L);
    }

    @Test
    void summarizeReport_shouldPersistRecordWithNullSourceReportId() {
        AiReportSummaryRequest request = new AiReportSummaryRequest();
        request.setProjectId(3L);
        request.setMarkdown("# Report");

        service.summarizeReport(request);

        ArgumentCaptor<AiAnalysisRecord> captor = ArgumentCaptor.forClass(AiAnalysisRecord.class);
        verify(aiAnalysisRecordService).saveRecordSafely(captor.capture());
        AiAnalysisRecord record = captor.getValue();
        assertThat(record.getAnalysisType()).isEqualTo("REPORT_SUMMARY");
        assertThat(record.getSourceReportId()).isNull();
        assertThat(record.getProjectId()).isEqualTo(3L);
    }

    @Test
    void summarizeReport_shouldSanitizeSensitiveDataInInputSummary() {
        AiReportSummaryRequest request = new AiReportSummaryRequest();
        request.setProjectId(1L);
        request.setMarkdown("api_key=sk-secret123\nNormal content");

        service.summarizeReport(request);

        ArgumentCaptor<AiAnalysisRecord> captor = ArgumentCaptor.forClass(AiAnalysisRecord.class);
        verify(aiAnalysisRecordService).saveRecordSafely(captor.capture());
        assertThat(captor.getValue().getInputSummary()).doesNotContain("sk-secret123");
        assertThat(captor.getValue().getInputSummary()).contains("***");
    }

    @Test
    void buildGitDiffMock_shouldReturnStandaloneMockWithoutPersistence() {
        AiGitDiffAnalysisRequest request = new AiGitDiffAnalysisRequest();
        request.setProjectId(1L);
        request.setGitAuditReportId(100L);

        AiGitDiffAnalysisVO vo = service.buildGitDiffMock(request);

        assertThat(vo.isMocked()).isTrue();
        assertThat(vo.getSummary()).isNotBlank();
        // Should NOT persist - this is a builder method
        org.mockito.Mockito.verifyNoInteractions(aiAnalysisRecordService);
    }

    @Test
    void buildRiskExplainMock_shouldReturnStandaloneMockWithoutPersistence() {
        AiRiskExplainRequest request = new AiRiskExplainRequest();
        request.setProjectId(1L);
        request.setReportId(200L);

        AiRiskExplainVO vo = service.buildRiskExplainMock(request);

        assertThat(vo.isMocked()).isTrue();
        assertThat(vo.getRiskSummary()).isNotBlank();
        org.mockito.Mockito.verifyNoInteractions(aiAnalysisRecordService);
    }

    @Test
    void buildReportSummaryMock_shouldReturnStandaloneMockWithoutPersistence() {
        AiReportSummaryRequest request = new AiReportSummaryRequest();
        request.setProjectId(1L);
        request.setMarkdown("# Report");

        AiReportSummaryVO vo = service.buildReportSummaryMock(request);

        assertThat(vo.isMocked()).isTrue();
        assertThat(vo.getExecutiveSummary()).isNotBlank();
        org.mockito.Mockito.verifyNoInteractions(aiAnalysisRecordService);
    }

    @Test
    void analyzeGitDiff_shouldContainChineseContent() {
        AiGitDiffAnalysisRequest request = new AiGitDiffAnalysisRequest();
        request.setProjectId(1L);
        request.setGitAuditReportId(100L);

        AiGitDiffAnalysisVO vo = service.analyzeGitDiff(request);

        assertThat(vo.getSummary()).contains("变更");
        assertThat(vo.getImpactAreas().get(0)).contains("影响");
        assertThat(vo.getTestSuggestions().get(0)).contains("建议");
    }

    @Test
    void explainRisk_shouldContainChineseContent() {
        AiRiskExplainRequest request = new AiRiskExplainRequest();
        request.setProjectId(1L);
        request.setReportId(200L);

        AiRiskExplainVO vo = service.explainRisk(request);

        assertThat(vo.getRiskSummary()).contains("风险");
        assertThat(vo.getWhyItMatters().get(0)).isNotBlank();
        assertThat(vo.getFixPlan().get(0)).isNotBlank();
    }

    @Test
    void summarizeReport_shouldContainChineseContent() {
        AiReportSummaryRequest request = new AiReportSummaryRequest();
        request.setProjectId(1L);
        request.setMarkdown("# Report");

        AiReportSummaryVO vo = service.summarizeReport(request);

        assertThat(vo.getExecutiveSummary()).contains("安全");
        assertThat(vo.getKeyFindings().get(0)).isNotBlank();
    }
}

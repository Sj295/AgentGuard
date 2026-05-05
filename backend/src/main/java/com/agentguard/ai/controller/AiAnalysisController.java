package com.agentguard.ai.controller;

import com.agentguard.ai.config.AiProperties;
import com.agentguard.ai.dto.AiGitDiffAnalysisRequest;
import com.agentguard.ai.dto.AiReportSummaryRequest;
import com.agentguard.ai.dto.AiRiskExplainRequest;
import com.agentguard.ai.service.AiAnalysisService;
import com.agentguard.ai.service.impl.LlmAiAnalysisServiceImpl;
import com.agentguard.ai.service.impl.MockAiAnalysisServiceImpl;
import com.agentguard.ai.vo.AiAnalysisRecordVO;
import com.agentguard.ai.vo.AiGitDiffAnalysisVO;
import com.agentguard.ai.vo.AiReportSummaryVO;
import com.agentguard.ai.vo.AiRiskExplainVO;
import com.agentguard.common.PageResult;
import com.agentguard.common.Result;
import com.agentguard.service.AiAnalysisRecordService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {

    private final AiProperties aiProperties;
    private final MockAiAnalysisServiceImpl mockAiAnalysisService;
    private final LlmAiAnalysisServiceImpl llmAiAnalysisService;
    private final AiAnalysisRecordService aiAnalysisRecordService;

    public AiAnalysisController(AiProperties aiProperties,
                                MockAiAnalysisServiceImpl mockAiAnalysisService,
                                LlmAiAnalysisServiceImpl llmAiAnalysisService,
                                AiAnalysisRecordService aiAnalysisRecordService) {
        this.aiProperties = aiProperties;
        this.mockAiAnalysisService = mockAiAnalysisService;
        this.llmAiAnalysisService = llmAiAnalysisService;
        this.aiAnalysisRecordService = aiAnalysisRecordService;
    }

    @PostMapping("/git-diff/analyze")
    public Result<AiGitDiffAnalysisVO> analyzeGitDiff(@Valid @RequestBody AiGitDiffAnalysisRequest request) {
        return Result.success(resolveService().analyzeGitDiff(request));
    }

    @PostMapping("/risk/explain")
    public Result<AiRiskExplainVO> explainRisk(@Valid @RequestBody AiRiskExplainRequest request) {
        return Result.success(resolveService().explainRisk(request));
    }

    @PostMapping("/report/summary")
    public Result<AiReportSummaryVO> summarizeReport(@Valid @RequestBody AiReportSummaryRequest request) {
        return Result.success(resolveService().summarizeReport(request));
    }

    @GetMapping("/records/project/{projectId}")
    public Result<PageResult<AiAnalysisRecordVO>> pageProjectRecords(@PathVariable Long projectId,
                                                                      @RequestParam(defaultValue = "1") long current,
                                                                      @RequestParam(defaultValue = "10") long size,
                                                                      @RequestParam(required = false) String analysisType) {
        return Result.success(aiAnalysisRecordService.pageProjectRecords(projectId, analysisType, current, size));
    }

    @GetMapping("/records/{id}")
    public Result<AiAnalysisRecordVO> getRecordDetail(@PathVariable Long id) {
        return Result.success(aiAnalysisRecordService.getRecordDetail(id));
    }

    @GetMapping("/records/project/{projectId}/latest")
    public Result<List<AiAnalysisRecordVO>> listLatestProjectRecords(@PathVariable Long projectId,
                                                                     @RequestParam(defaultValue = "5") int limit) {
        return Result.success(aiAnalysisRecordService.listLatestProjectRecords(projectId, limit));
    }

    private AiAnalysisService resolveService() {
        if (!aiProperties.isEnabled()) {
            return mockAiAnalysisService;
        }
        if (!aiProperties.hasApiKey()) {
            if (aiProperties.isMockOnEmptyKey()) {
                return mockAiAnalysisService;
            }
            return llmAiAnalysisService;
        }
        return llmAiAnalysisService;
    }
}

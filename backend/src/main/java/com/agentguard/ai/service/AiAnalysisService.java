package com.agentguard.ai.service;

import com.agentguard.ai.dto.AiGitDiffAnalysisRequest;
import com.agentguard.ai.dto.AiReportSummaryRequest;
import com.agentguard.ai.dto.AiRiskExplainRequest;
import com.agentguard.ai.vo.AiGitDiffAnalysisVO;
import com.agentguard.ai.vo.AiReportSummaryVO;
import com.agentguard.ai.vo.AiRiskExplainVO;

public interface AiAnalysisService {

    String CONFIDENCE_NOTE = "AI 增强建议，仅供参考，最终风险等级以规则引擎结果为准。";

    AiGitDiffAnalysisVO analyzeGitDiff(AiGitDiffAnalysisRequest request);

    AiRiskExplainVO explainRisk(AiRiskExplainRequest request);

    AiReportSummaryVO summarizeReport(AiReportSummaryRequest request);
}

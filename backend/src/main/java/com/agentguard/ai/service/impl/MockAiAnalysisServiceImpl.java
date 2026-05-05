package com.agentguard.ai.service.impl;

import com.agentguard.ai.dto.AiGitDiffAnalysisRequest;
import com.agentguard.ai.dto.AiReportSummaryRequest;
import com.agentguard.ai.dto.AiRiskExplainRequest;
import com.agentguard.ai.service.AiAnalysisService;
import com.agentguard.ai.vo.AiGitDiffAnalysisVO;
import com.agentguard.ai.vo.AiReportSummaryVO;
import com.agentguard.ai.vo.AiRiskExplainVO;
import com.agentguard.common.JsonUtils;
import com.agentguard.common.enums.AiAnalysisType;
import com.agentguard.entity.AiAnalysisRecord;
import com.agentguard.service.AiAnalysisRecordService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service("mockAiAnalysisService")
public class MockAiAnalysisServiceImpl implements AiAnalysisService {

    private static final String MOCK_PROVIDER = "mock";
    private static final String MOCK_MODEL = "mock-model";
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(api[_-]?key|token|password|secret|private[_-]?key)\\s*[:=]\\s*[^\\s]+"
    );
    private static final int SUMMARY_MAX_LENGTH = 300;

    private final AiAnalysisRecordService aiAnalysisRecordService;

    public MockAiAnalysisServiceImpl(AiAnalysisRecordService aiAnalysisRecordService) {
        this.aiAnalysisRecordService = aiAnalysisRecordService;
    }

    @Override
    public AiGitDiffAnalysisVO analyzeGitDiff(AiGitDiffAnalysisRequest request) {
        long start = System.currentTimeMillis();
        AiGitDiffAnalysisVO vo = buildGitDiffMock(request);
        persistRecord(
                AiAnalysisType.GIT_DIFF_ANALYSIS,
                request.getProjectId(),
                request.getGitAuditReportId(),
                buildGitDiffInputSummary(request),
                JsonUtils.toJson(vo),
                null,
                System.currentTimeMillis() - start
        );
        return vo;
    }

    public AiGitDiffAnalysisVO buildGitDiffMock(AiGitDiffAnalysisRequest request) {
        AiGitDiffAnalysisVO vo = new AiGitDiffAnalysisVO();
        vo.setProjectId(request.getProjectId());
        vo.setGitAuditReportId(request.getGitAuditReportId());
        vo.setSummary("本次变更主要涉及配置与业务代码调整，建议重点关注构建链路、依赖兼容性和关键接口稳定性。");
        vo.setImpactAreas(List.of(
                "可能影响后端启动配置与环境变量读取",
                "可能影响前端构建与静态资源打包流程",
                "可能影响关键 API 调用链与错误处理路径"
        ));
        vo.setTestSuggestions(List.of(
                "建议先运行 mvn test 验证后端核心逻辑",
                "建议运行 npm run build 验证前端构建链路",
                "建议执行关键接口的冒烟测试与回归测试"
        ));
        vo.setRollbackSuggestions(List.of(
                "若出现异常，可优先使用 git restore 恢复关键变更文件",
                "建议基于最近稳定提交创建回滚分支并逐步恢复",
                "在回滚前记录问题现象与日志，避免重复引入缺陷"
        ));
        vo.setConfidenceNote(CONFIDENCE_NOTE);
        vo.setMocked(true);
        return vo;
    }

    @Override
    public AiRiskExplainVO explainRisk(AiRiskExplainRequest request) {
        long start = System.currentTimeMillis();
        AiRiskExplainVO vo = buildRiskExplainMock(request);
        persistRecord(
                AiAnalysisType.RISK_EXPLAIN,
                request.getProjectId(),
                request.getReportId(),
                buildRiskExplainInputSummary(request),
                JsonUtils.toJson(vo),
                null,
                System.currentTimeMillis() - start
        );
        return vo;
    }

    public AiRiskExplainVO buildRiskExplainMock(AiRiskExplainRequest request) {
        AiRiskExplainVO vo = new AiRiskExplainVO();
        vo.setProjectId(request.getProjectId());
        vo.setReportId(request.getReportId());
        vo.setRiskSummary("当前风险主要集中在高权限操作与高危命令组合，若直接执行可能扩大变更影响范围。");
        vo.setWhyItMatters(List.of(
                "审批策略过宽会提升误操作与越权修改概率",
                "危险命令可能导致不可逆的数据或文件损坏",
                "未充分预检会增加上线后回滚成本"
        ));
        vo.setFixPlan(List.of(
                "优先将 approvalPolicy 调整为 ON_REQUEST",
                "移除或替换高危命令，改用可审计的安全替代方案",
                "重新执行 Preflight Check 与 Git Diff 审计确认风险收敛"
        ));
        vo.setSafeNextSteps(List.of(
                "先检查 Git 工作区并确认待变更文件范围",
                "再执行最小化改动并进行分步验证",
                "最后生成并写入 Agent 规则文件后再继续任务"
        ));
        vo.setConfidenceNote(CONFIDENCE_NOTE);
        vo.setMocked(true);
        return vo;
    }

    @Override
    public AiReportSummaryVO summarizeReport(AiReportSummaryRequest request) {
        long start = System.currentTimeMillis();
        AiReportSummaryVO vo = buildReportSummaryMock(request);
        persistRecord(
                AiAnalysisType.REPORT_SUMMARY,
                request.getProjectId(),
                null,
                buildReportSummaryInputSummary(request),
                JsonUtils.toJson(vo),
                null,
                System.currentTimeMillis() - start
        );
        return vo;
    }

    public AiReportSummaryVO buildReportSummaryMock(AiReportSummaryRequest request) {
        AiReportSummaryVO vo = new AiReportSummaryVO();
        vo.setProjectId(request.getProjectId());
        vo.setExecutiveSummary("项目已形成完整安全治理闭环，当前重点在于持续降低高风险事件并强化执行前预检。");
        vo.setKeyFindings(List.of(
                "项目已完成扫描、规则生成和多维审计流程",
                "风险事件可追溯到权限配置、命令执行与 Git 变更",
                "安全时间线可用于定位高风险峰值与整改进度"
        ));
        vo.setPriorityActions(List.of(
                "优先处理 CRITICAL/HIGH 风险报告并验证修复效果",
                "在每次 Agent 执行前固定执行 Preflight Check",
                "将关键变更纳入分支保护与回滚预案流程"
        ));
        vo.setConfidenceNote(CONFIDENCE_NOTE);
        vo.setMocked(true);
        return vo;
    }

    private void persistRecord(AiAnalysisType analysisType,
                               Long projectId,
                               Long sourceReportId,
                               String inputSummary,
                               String outputContent,
                               String errorMessage,
                               long latencyMs) {
        AiAnalysisRecord record = new AiAnalysisRecord();
        record.setProjectId(projectId);
        record.setAnalysisType(analysisType.name());
        record.setSourceReportId(sourceReportId);
        record.setProvider(MOCK_PROVIDER);
        record.setModel(MOCK_MODEL);
        record.setMocked(true);
        record.setSuccess(true);
        record.setLatencyMs(Math.max(0, latencyMs));
        record.setInputSummary(inputSummary);
        record.setOutputContent(outputContent);
        record.setErrorMessage(errorMessage);
        record.setCreatedTime(LocalDateTime.now());
        aiAnalysisRecordService.saveRecordSafely(record);
    }

    private String buildGitDiffInputSummary(AiGitDiffAnalysisRequest request) {
        return "projectId=" + request.getProjectId() + ", gitAuditReportId=" + request.getGitAuditReportId();
    }

    private String buildRiskExplainInputSummary(AiRiskExplainRequest request) {
        return "projectId=" + request.getProjectId() + ", reportId=" + request.getReportId();
    }

    private String buildReportSummaryInputSummary(AiReportSummaryRequest request) {
        String markdown = request.getMarkdown();
        int length = markdown == null ? 0 : markdown.length();
        String snippet = "";
        if (StringUtils.hasText(markdown)) {
            snippet = sanitize(markdown.substring(0, Math.min(markdown.length(), SUMMARY_MAX_LENGTH)));
        }
        return "projectId=" + request.getProjectId() + ", markdownLength=" + length + ", snippet=" + snippet;
    }

    private String sanitize(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return SENSITIVE_PATTERN.matcher(text).replaceAll("$1=***");
    }
}

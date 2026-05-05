package com.agentguard.ai.service.impl;

import com.agentguard.ai.client.LlmClient;
import com.agentguard.ai.config.AiProperties;
import com.agentguard.ai.dto.AiGitDiffAnalysisRequest;
import com.agentguard.ai.dto.AiReportSummaryRequest;
import com.agentguard.ai.dto.AiRiskExplainRequest;
import com.agentguard.ai.prompt.GitDiffPromptBuilder;
import com.agentguard.ai.prompt.PromptBundle;
import com.agentguard.ai.prompt.ReportSummaryPromptBuilder;
import com.agentguard.ai.prompt.RiskExplainPromptBuilder;
import com.agentguard.ai.service.AiAnalysisService;
import com.agentguard.ai.vo.AiGitDiffAnalysisVO;
import com.agentguard.ai.vo.AiReportSummaryVO;
import com.agentguard.ai.vo.AiRiskExplainVO;
import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.common.JsonUtils;
import com.agentguard.common.enums.AiAnalysisType;
import com.agentguard.common.enums.RiskReportType;
import com.agentguard.entity.AiAnalysisRecord;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.RiskReport;
import com.agentguard.service.AiAnalysisRecordService;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.RiskReportService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;

@Service("llmAiAnalysisService")
public class LlmAiAnalysisServiceImpl implements AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(LlmAiAnalysisServiceImpl.class);
    private static final int MAX_MARKDOWN_LENGTH = 12000;
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*(\\{.*?})\\s*```", Pattern.DOTALL);
    private static final Pattern DANGEROUS_COMMAND_PATTERN = Pattern.compile(
            "(?i)(rm\\s+-rf|git\\s+reset\\s+--hard|mkfs\\b|dd\\s+if=|curl\\s+[^\\n]*\\|\\s*(sh|bash)|wget\\s+[^\\n]*\\|\\s*(sh|bash))"
    );
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(api[_-]?key|token|password|secret|private[_-]?key)\\s*[:=]\\s*[^\\s]+"
    );
    private static final Set<String> EXPLAINABLE_TYPES = Set.of(
            RiskReportType.PERMISSION_ASSESS.name(),
            RiskReportType.COMMAND_AUDIT.name(),
            RiskReportType.PREFLIGHT_CHECK.name(),
            RiskReportType.GIT_DIFF_AUDIT.name()
    );

    private final ProjectInfoService projectInfoService;
    private final RiskReportService riskReportService;
    private final AiAnalysisRecordService aiAnalysisRecordService;
    private final LlmClient llmClient;
    private final AiProperties aiProperties;
    private final GitDiffPromptBuilder gitDiffPromptBuilder;
    private final RiskExplainPromptBuilder riskExplainPromptBuilder;
    private final ReportSummaryPromptBuilder reportSummaryPromptBuilder;
    private final MockAiAnalysisServiceImpl mockAiAnalysisService;
    private final ObjectMapper objectMapper;

    public LlmAiAnalysisServiceImpl(ProjectInfoService projectInfoService,
                                    RiskReportService riskReportService,
                                    AiAnalysisRecordService aiAnalysisRecordService,
                                    LlmClient llmClient,
                                    AiProperties aiProperties,
                                    GitDiffPromptBuilder gitDiffPromptBuilder,
                                    RiskExplainPromptBuilder riskExplainPromptBuilder,
                                    ReportSummaryPromptBuilder reportSummaryPromptBuilder,
                                    MockAiAnalysisServiceImpl mockAiAnalysisService,
                                    ObjectMapper objectMapper) {
        this.projectInfoService = projectInfoService;
        this.riskReportService = riskReportService;
        this.aiAnalysisRecordService = aiAnalysisRecordService;
        this.llmClient = llmClient;
        this.aiProperties = aiProperties;
        this.gitDiffPromptBuilder = gitDiffPromptBuilder;
        this.riskExplainPromptBuilder = riskExplainPromptBuilder;
        this.reportSummaryPromptBuilder = reportSummaryPromptBuilder;
        this.mockAiAnalysisService = mockAiAnalysisService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiGitDiffAnalysisVO analyzeGitDiff(AiGitDiffAnalysisRequest request) {
        long start = System.currentTimeMillis();
        ProjectInfo projectInfo = loadProject(request.getProjectId());
        RiskReport report = loadReport(request.getGitAuditReportId());
        validateReportBelongsToProject(report, request.getProjectId());
        if (!RiskReportType.GIT_DIFF_AUDIT.name().equals(report.getReportType())) {
            throw new BusinessException(ErrorCode.INVALID_REPORT_TYPE, "Report type is not GIT_DIFF_AUDIT");
        }

        List<String> techStack = parseStringList(projectInfo.getTechStack());
        List<String> riskItems = parseStringList(report.getRiskItems());
        List<String> suggestions = parseStringList(report.getSuggestions());
        List<String> promptSuggestions = new ArrayList<>(suggestions);
        if (StringUtils.hasText(report.getSummary())) {
            promptSuggestions.add("reportSummary: " + report.getSummary().trim());
        }
        List<String> changedFiles = extractChangedFiles(report.getPayloadJson());
        if (!changedFiles.isEmpty()) {
            promptSuggestions.add("changedFiles: " + changedFiles);
        }
        String inputSummary = buildGitDiffInputSummary(projectInfo, report, techStack, riskItems, promptSuggestions, changedFiles);
        PromptBundle prompt = gitDiffPromptBuilder.build(
                projectInfo.getProjectName(),
                projectInfo.getProjectType(),
                techStack,
                riskItems,
                promptSuggestions
        );
        try {
            String content = llmClient.chat(prompt.systemPrompt(), prompt.userPrompt());
            AiGitDiffAnalysisVO vo = parseGitDiffContent(content, riskItems, suggestions);
            vo.setProjectId(request.getProjectId());
            vo.setGitAuditReportId(request.getGitAuditReportId());
            vo.setConfidenceNote(CONFIDENCE_NOTE);
            vo.setMocked(false);
            persistRecord(
                    AiAnalysisType.GIT_DIFF_ANALYSIS,
                    request.getProjectId(),
                    request.getGitAuditReportId(),
                    aiProperties.getProvider(),
                    aiProperties.getModel(),
                    false,
                    true,
                    System.currentTimeMillis() - start,
                    inputSummary,
                    JsonUtils.toJson(vo),
                    null
            );
            return vo;
        } catch (RuntimeException exception) {
            log.warn("AI git diff analysis degraded to mock: {}", exception.getMessage());
            AiGitDiffAnalysisVO fallback = mockAiAnalysisService.buildGitDiffMock(request);
            persistRecord(
                    AiAnalysisType.GIT_DIFF_ANALYSIS,
                    request.getProjectId(),
                    request.getGitAuditReportId(),
                    fallbackProvider(),
                    configuredModel(),
                    true,
                    false,
                    System.currentTimeMillis() - start,
                    inputSummary,
                    JsonUtils.toJson(fallback),
                    trimError(exception.getMessage())
            );
            return fallback;
        }
    }

    @Override
    public AiRiskExplainVO explainRisk(AiRiskExplainRequest request) {
        long start = System.currentTimeMillis();
        loadProject(request.getProjectId());
        RiskReport report = loadReport(request.getReportId());
        validateReportBelongsToProject(report, request.getProjectId());
        if (!EXPLAINABLE_TYPES.contains(report.getReportType())) {
            throw new BusinessException(ErrorCode.INVALID_REPORT_TYPE,
                    "Report type must be one of: PERMISSION_ASSESS, COMMAND_AUDIT, PREFLIGHT_CHECK, GIT_DIFF_AUDIT");
        }

        List<String> riskItems = parseStringList(report.getRiskItems());
        List<String> suggestions = parseStringList(report.getSuggestions());
        String inputSummary = buildRiskExplainInputSummary(report, riskItems, suggestions);
        PromptBundle prompt = riskExplainPromptBuilder.build(
                report.getReportType(),
                report.getRiskLevel(),
                riskItems,
                suggestions
        );
        try {
            String content = llmClient.chat(prompt.systemPrompt(), prompt.userPrompt());
            AiRiskExplainVO vo = parseRiskExplainContent(content, suggestions);
            vo.setProjectId(request.getProjectId());
            vo.setReportId(request.getReportId());
            vo.setConfidenceNote(CONFIDENCE_NOTE);
            vo.setMocked(false);
            persistRecord(
                    AiAnalysisType.RISK_EXPLAIN,
                    request.getProjectId(),
                    request.getReportId(),
                    aiProperties.getProvider(),
                    aiProperties.getModel(),
                    false,
                    true,
                    System.currentTimeMillis() - start,
                    inputSummary,
                    JsonUtils.toJson(vo),
                    null
            );
            return vo;
        } catch (RuntimeException exception) {
            log.warn("AI risk explanation degraded to mock: {}", exception.getMessage());
            AiRiskExplainVO fallback = mockAiAnalysisService.buildRiskExplainMock(request);
            persistRecord(
                    AiAnalysisType.RISK_EXPLAIN,
                    request.getProjectId(),
                    request.getReportId(),
                    fallbackProvider(),
                    configuredModel(),
                    true,
                    false,
                    System.currentTimeMillis() - start,
                    inputSummary,
                    JsonUtils.toJson(fallback),
                    trimError(exception.getMessage())
            );
            return fallback;
        }
    }

    @Override
    public AiReportSummaryVO summarizeReport(AiReportSummaryRequest request) {
        long start = System.currentTimeMillis();
        loadProject(request.getProjectId());
        String truncatedMarkdown = truncateMarkdown(request.getMarkdown());
        String inputSummary = buildReportSummaryInputSummary(request.getProjectId(), request.getMarkdown(), truncatedMarkdown);
        PromptBundle prompt = reportSummaryPromptBuilder.build(truncatedMarkdown);
        try {
            String content = llmClient.chat(prompt.systemPrompt(), prompt.userPrompt());
            AiReportSummaryVO vo = parseReportSummaryContent(content);
            vo.setProjectId(request.getProjectId());
            vo.setConfidenceNote(CONFIDENCE_NOTE);
            vo.setMocked(false);
            persistRecord(
                    AiAnalysisType.REPORT_SUMMARY,
                    request.getProjectId(),
                    null,
                    aiProperties.getProvider(),
                    aiProperties.getModel(),
                    false,
                    true,
                    System.currentTimeMillis() - start,
                    inputSummary,
                    JsonUtils.toJson(vo),
                    null
            );
            return vo;
        } catch (RuntimeException exception) {
            log.warn("AI markdown summary degraded to mock: {}", exception.getMessage());
            AiReportSummaryVO fallback = mockAiAnalysisService.buildReportSummaryMock(request);
            persistRecord(
                    AiAnalysisType.REPORT_SUMMARY,
                    request.getProjectId(),
                    null,
                    fallbackProvider(),
                    configuredModel(),
                    true,
                    false,
                    System.currentTimeMillis() - start,
                    inputSummary,
                    JsonUtils.toJson(fallback),
                    trimError(exception.getMessage())
            );
            return fallback;
        }
    }

    private ProjectInfo loadProject(Long projectId) {
        if (projectId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Project id cannot be null");
        }
        ProjectInfo projectInfo = projectInfoService.getById(projectId);
        if (projectInfo == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return projectInfo;
    }

    private RiskReport loadReport(Long reportId) {
        if (reportId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Report id cannot be null");
        }
        RiskReport report = riskReportService.getById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        return report;
    }

    private void validateReportBelongsToProject(RiskReport report, Long projectId) {
        if (report.getProjectId() == null || !report.getProjectId().equals(projectId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Report does not belong to the specified project");
        }
    }

    private AiGitDiffAnalysisVO parseGitDiffContent(String content, List<String> riskItems, List<String> suggestions) {
        List<String> fallbackImpact = riskItems.isEmpty()
                ? List.of("建议结合 Git 审计结果重点复核关键模块变更范围")
                : riskItems;
        List<String> fallbackTest = suggestions.isEmpty()
                ? List.of("建议执行后端和前端基础构建测试")
                : suggestions;
        List<String> fallbackRollback = List.of("如发现异常，优先使用 git restore 恢复关键文件，再逐步定位问题");

        AiGitDiffAnalysisVO vo = new AiGitDiffAnalysisVO();
        JsonNode json = extractJsonNode(content);
        if (json != null) {
            vo.setSummary(readText(json, "summary", "本次变更影响分析已完成，建议按风险项逐项验证。"));
            vo.setImpactAreas(readStringList(json, "impactAreas", fallbackImpact));
            vo.setTestSuggestions(sanitizeNonDestructive(readStringList(json, "testSuggestions", fallbackTest), fallbackTest));
            vo.setRollbackSuggestions(sanitizeNonDestructive(readStringList(json, "rollbackSuggestions", fallbackRollback), fallbackRollback));
            return vo;
        }
        vo.setSummary(fallbackTextSummary(content, "本次变更影响分析已完成，建议按风险项逐项验证。"));
        vo.setImpactAreas(fallbackImpact);
        vo.setTestSuggestions(sanitizeNonDestructive(fallbackTest, fallbackTest));
        vo.setRollbackSuggestions(fallbackRollback);
        return vo;
    }

    private AiRiskExplainVO parseRiskExplainContent(String content, List<String> suggestions) {
        List<String> fallbackFixPlan = suggestions.isEmpty()
                ? List.of("建议先收敛配置权限，再进行复测验证")
                : suggestions;
        List<String> fallbackNextSteps = List.of(
                "先检查 Git 工作区与待变更范围",
                "再按最小变更原则修复并执行回归验证"
        );

        AiRiskExplainVO vo = new AiRiskExplainVO();
        JsonNode json = extractJsonNode(content);
        if (json != null) {
            vo.setRiskSummary(readText(json, "riskSummary", "当前风险需要通过配置收敛和执行前复核来降低。"));
            vo.setWhyItMatters(readStringList(json, "whyItMatters",
                    List.of("若忽略当前风险，可能扩大变更影响与回滚成本")));
            vo.setFixPlan(sanitizeNonDestructive(readStringList(json, "fixPlan", fallbackFixPlan), fallbackFixPlan));
            vo.setSafeNextSteps(sanitizeNonDestructive(readStringList(json, "safeNextSteps", fallbackNextSteps), fallbackNextSteps));
            return vo;
        }
        vo.setRiskSummary(fallbackTextSummary(content, "当前风险需要通过配置收敛和执行前复核来降低。"));
        vo.setWhyItMatters(List.of("若忽略当前风险，可能扩大变更影响与回滚成本"));
        vo.setFixPlan(sanitizeNonDestructive(fallbackFixPlan, fallbackFixPlan));
        vo.setSafeNextSteps(fallbackNextSteps);
        return vo;
    }

    private AiReportSummaryVO parseReportSummaryContent(String content) {
        AiReportSummaryVO vo = new AiReportSummaryVO();
        JsonNode json = extractJsonNode(content);
        if (json != null) {
            vo.setExecutiveSummary(readText(json, "executiveSummary", "已完成报告摘要，建议优先处理高风险事件。"));
            vo.setKeyFindings(readStringList(json, "keyFindings",
                    List.of("报告包含关键风险事件与治理建议")));
            vo.setPriorityActions(sanitizeNonDestructive(readStringList(json, "priorityActions",
                            List.of("优先处理 CRITICAL/HIGH 风险项并复测")),
                    List.of("优先处理 CRITICAL/HIGH 风险项并复测")));
            return vo;
        }
        vo.setExecutiveSummary(fallbackTextSummary(content, "已完成报告摘要，建议优先处理高风险事件。"));
        vo.setKeyFindings(List.of("报告包含关键风险事件与治理建议"));
        vo.setPriorityActions(List.of("优先处理 CRITICAL/HIGH 风险项并复测"));
        return vo;
    }

    private JsonNode extractJsonNode(String content) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        try {
            JsonNode direct = objectMapper.readTree(content);
            if (direct.isObject()) {
                return direct;
            }
        } catch (Exception ignored) {
            // fall through to wrapped-json extraction
        }

        Matcher matcher = JSON_BLOCK_PATTERN.matcher(content);
        if (matcher.find()) {
            String jsonBlock = matcher.group(1);
            try {
                return objectMapper.readTree(jsonBlock);
            } catch (Exception ignored) {
                // fall through to brace-based extraction
            }
        }

        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String json = content.substring(start, end + 1);
            try {
                return objectMapper.readTree(json);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private String readText(JsonNode node, String field, String fallback) {
        if (node == null) {
            return fallback;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        String text = value.asText();
        if (!StringUtils.hasText(text)) {
            return fallback;
        }
        return text.trim();
    }

    private List<String> readStringList(JsonNode node, String field, List<String> fallback) {
        if (node == null) {
            return fallback;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return fallback;
        }
        if (value.isArray()) {
            List<String> result = new ArrayList<>();
            value.forEach(item -> {
                String text = item.asText();
                if (StringUtils.hasText(text)) {
                    result.add(text.trim());
                }
            });
            return result.isEmpty() ? fallback : result;
        }
        if (value.isTextual() && StringUtils.hasText(value.asText())) {
            return List.of(value.asText().trim());
        }
        return fallback;
    }

    private List<String> sanitizeNonDestructive(List<String> source, List<String> fallback) {
        List<String> result = new ArrayList<>();
        for (String item : source) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            if (!DANGEROUS_COMMAND_PATTERN.matcher(item).find()) {
                result.add(item.trim());
            }
        }
        return result.isEmpty() ? fallback : result;
    }

    private String truncateMarkdown(String markdown) {
        if (markdown == null) {
            return "";
        }
        String normalized = markdown.trim();
        if (normalized.length() <= MAX_MARKDOWN_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_MARKDOWN_LENGTH);
    }

    private List<String> parseStringList(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        try {
            return JsonUtils.parseStringList(value);
        } catch (RuntimeException exception) {
            String[] pieces = value.split("\\R|,|;");
            List<String> parsed = new ArrayList<>();
            for (String piece : pieces) {
                if (StringUtils.hasText(piece)) {
                    parsed.add(piece.trim());
                }
            }
            return parsed;
        }
    }

    private String fallbackTextSummary(String raw, String defaultText) {
        if (!StringUtils.hasText(raw)) {
            return defaultText;
        }
        String text = raw.trim();
        if (text.length() <= 240) {
            return text;
        }
        return text.substring(0, 240);
    }

    private void persistRecord(AiAnalysisType analysisType,
                               Long projectId,
                               Long sourceReportId,
                               String provider,
                               String model,
                               boolean mocked,
                               boolean success,
                               long latencyMs,
                               String inputSummary,
                               String outputContent,
                               String errorMessage) {
        AiAnalysisRecord record = new AiAnalysisRecord();
        record.setProjectId(projectId);
        record.setAnalysisType(analysisType.name());
        record.setSourceReportId(sourceReportId);
        record.setProvider(StringUtils.hasText(provider) ? provider : "spring-ai-openai-compatible");
        record.setModel(StringUtils.hasText(model) ? model : "unknown-model");
        record.setMocked(mocked);
        record.setSuccess(success);
        record.setLatencyMs(Math.max(0, latencyMs));
        record.setInputSummary(trimToLength(sanitize(inputSummary), 1500));
        record.setOutputContent(outputContent);
        record.setErrorMessage(trimError(errorMessage));
        record.setCreatedTime(LocalDateTime.now());
        aiAnalysisRecordService.saveRecordSafely(record);
    }

    private String buildGitDiffInputSummary(ProjectInfo projectInfo,
                                            RiskReport report,
                                            List<String> techStack,
                                            List<String> riskItems,
                                            List<String> suggestions,
                                            List<String> changedFiles) {
        return "projectName=" + safe(projectInfo.getProjectName())
                + ", projectType=" + safe(projectInfo.getProjectType())
                + ", reportSummary=" + safe(report.getSummary())
                + ", techStack=" + compactList(techStack, 8)
                + ", riskItems=" + compactList(riskItems, 8)
                + ", suggestions=" + compactList(suggestions, 8)
                + ", changedFiles=" + compactList(changedFiles, 20);
    }

    private String buildRiskExplainInputSummary(RiskReport report, List<String> riskItems, List<String> suggestions) {
        return "reportType=" + safe(report.getReportType())
                + ", riskLevel=" + safe(report.getRiskLevel())
                + ", reportSummary=" + safe(report.getSummary())
                + ", riskItems=" + compactList(riskItems, 10)
                + ", suggestions=" + compactList(suggestions, 10);
    }

    private String buildReportSummaryInputSummary(Long projectId, String originalMarkdown, String truncatedMarkdown) {
        int originalLength = originalMarkdown == null ? 0 : originalMarkdown.length();
        int usedLength = truncatedMarkdown == null ? 0 : truncatedMarkdown.length();
        String snippet = "";
        if (StringUtils.hasText(truncatedMarkdown)) {
            snippet = truncatedMarkdown.substring(0, Math.min(truncatedMarkdown.length(), 300));
        }
        return "projectId=" + projectId
                + ", markdownLength=" + originalLength
                + ", usedLength=" + usedLength
                + ", snippet=" + sanitize(snippet);
    }

    private String compactList(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        List<String> compact = values.stream()
                .filter(StringUtils::hasText)
                .map(this::sanitize)
                .map(item -> trimToLength(item, 120))
                .limit(Math.max(1, limit))
                .toList();
        return compact.toString();
    }

    private String sanitize(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return SENSITIVE_PATTERN.matcher(text).replaceAll("$1=***");
    }

    private String safe(String text) {
        if (!StringUtils.hasText(text)) {
            return "UNKNOWN";
        }
        return trimToLength(sanitize(text.trim()), 160);
    }

    private String trimToLength(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private String trimError(String error) {
        if (!StringUtils.hasText(error)) {
            return null;
        }
        return trimToLength(error, 1000);
    }

    private String fallbackProvider() {
        return "fallback-mock";
    }

    private String configuredModel() {
        if (!StringUtils.hasText(aiProperties.getModel())) {
            return "unknown-model";
        }
        return aiProperties.getModel().trim();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractChangedFiles(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return List.of();
        }
        try {
            Map<String, Object> payload = JsonUtils.parseMap(payloadJson);
            LinkedHashSet<String> files = new LinkedHashSet<>();
            appendFiles(files, payload.get("addedFiles"));
            appendFiles(files, payload.get("modifiedFiles"));
            appendFiles(files, payload.get("deletedFiles"));
            return files.stream().limit(30).toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private void appendFiles(LinkedHashSet<String> files, Object value) {
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && StringUtils.hasText(item.toString())) {
                    files.add(item.toString().trim());
                }
            }
            return;
        }
        if (value != null && StringUtils.hasText(value.toString())) {
            files.add(value.toString().trim());
        }
    }
}

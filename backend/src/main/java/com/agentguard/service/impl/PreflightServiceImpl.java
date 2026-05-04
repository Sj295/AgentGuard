package com.agentguard.service.impl;

import com.agentguard.audit.GitCommandExecutor;
import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.common.JsonUtils;
import com.agentguard.common.PageResult;
import com.agentguard.common.enums.AgentType;
import com.agentguard.common.enums.ApprovalPolicy;
import com.agentguard.common.enums.RiskReportType;
import com.agentguard.common.enums.SandboxMode;
import com.agentguard.common.enums.TaskType;
import com.agentguard.dto.PreflightCheckRequest;
import com.agentguard.entity.AgentRule;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.RiskReport;
import com.agentguard.entity.ScanResult;
import com.agentguard.preflight.PreflightCheckContext;
import com.agentguard.preflight.PreflightCheckResult;
import com.agentguard.preflight.PreflightChecker;
import com.agentguard.service.AgentRuleService;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.PreflightService;
import com.agentguard.service.RiskReportService;
import com.agentguard.service.ScanResultService;
import com.agentguard.vo.PreflightCheckItemVO;
import com.agentguard.vo.PreflightCheckVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PreflightServiceImpl implements PreflightService {

    private static final Logger log = LoggerFactory.getLogger(PreflightServiceImpl.class);
    private static final String REPORT_TYPE_PREFLIGHT_CHECK = RiskReportType.PREFLIGHT_CHECK.name();

    private final ProjectInfoService projectInfoService;
    private final ScanResultService scanResultService;
    private final AgentRuleService agentRuleService;
    private final RiskReportService riskReportService;
    private final PreflightChecker preflightChecker;
    private final GitCommandExecutor gitCommandExecutor;

    public PreflightServiceImpl(ProjectInfoService projectInfoService,
                                ScanResultService scanResultService,
                                AgentRuleService agentRuleService,
                                RiskReportService riskReportService,
                                PreflightChecker preflightChecker,
                                GitCommandExecutor gitCommandExecutor) {
        this.projectInfoService = projectInfoService;
        this.scanResultService = scanResultService;
        this.agentRuleService = agentRuleService;
        this.riskReportService = riskReportService;
        this.preflightChecker = preflightChecker;
        this.gitCommandExecutor = gitCommandExecutor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PreflightCheckVO check(PreflightCheckRequest request) {
        ProjectInfo projectInfo = loadProjectInfo(request.getProjectId());
        AgentType agentType = AgentType.fromCode(request.getAgentType());
        TaskType taskType = TaskType.fromCode(request.getTaskType());
        SandboxMode sandboxMode = SandboxMode.fromCode(request.getSandboxMode());
        ApprovalPolicy approvalPolicy = ApprovalPolicy.fromCode(request.getApprovalPolicy());

        ScanResult latestScanResult = scanResultService.lambdaQuery()
                .eq(ScanResult::getProjectId, request.getProjectId())
                .orderByDesc(ScanResult::getCreatedTime)
                .orderByDesc(ScanResult::getId)
                .last("limit 1")
                .one();

        AgentRule latestAgentRule = agentRuleService.lambdaQuery()
                .eq(AgentRule::getProjectId, request.getProjectId())
                .eq(AgentRule::getAgentType, agentType.getCode())
                .orderByDesc(AgentRule::getUpdatedTime)
                .orderByDesc(AgentRule::getCreatedTime)
                .orderByDesc(AgentRule::getId)
                .last("limit 1")
                .one();

        boolean isGitRepo = detectGitRepository(projectInfo);
        int changedFileCount = 0;
        if (isGitRepo) {
            changedFileCount = detectChangedFileCount(projectInfo);
        }

        boolean ruleFileExists = checkRuleFileOnDisk(projectInfo, agentType);

        List<String> sensitiveFiles = List.of();
        if (latestScanResult != null) {
            sensitiveFiles = JsonUtils.parseStringList(latestScanResult.getSensitiveFiles());
        }

        PreflightCheckContext context = PreflightCheckContext.builder()
                .projectId(request.getProjectId())
                .agentType(agentType)
                .taskType(taskType)
                .sandboxMode(sandboxMode)
                .approvalPolicy(approvalPolicy)
                .networkAccess(Boolean.TRUE.equals(request.getNetworkAccess()))
                .allowDelete(Boolean.TRUE.equals(request.getAllowDelete()))
                .plannedCommands(request.getPlannedCommands())
                .projectInfo(projectInfo)
                .latestScanResult(latestScanResult)
                .hasScanResult(latestScanResult != null)
                .latestAgentRule(latestAgentRule)
                .hasAgentRule(latestAgentRule != null)
                .ruleFileExistsOnDisk(ruleFileExists)
                .isGitRepository(isGitRepo)
                .changedFileCount(changedFileCount)
                .hasGitChanges(changedFileCount > 0)
                .sensitiveFiles(sensitiveFiles)
                .build();

        PreflightCheckResult result = preflightChecker.check(context);
        RiskReport report = saveReport(request, result);
        return toVO(request, result, report.getCreatedTime());
    }

    @Override
    public PageResult<PreflightCheckVO> pageProjectReports(Long projectId, long current, long size) {
        loadProjectInfo(projectId);
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Pagination parameters must be greater than 0");
        }
        Page<RiskReport> page = riskReportService.page(
                new Page<>(current, size),
                Wrappers.<RiskReport>lambdaQuery()
                        .eq(RiskReport::getProjectId, projectId)
                        .eq(RiskReport::getReportType, REPORT_TYPE_PREFLIGHT_CHECK)
                        .orderByDesc(RiskReport::getCreatedTime)
                        .orderByDesc(RiskReport::getId)
        );
        Page<PreflightCheckVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::parseReport).toList());
        return PageResult.fromPage(voPage);
    }

    private ProjectInfo loadProjectInfo(Long projectId) {
        if (projectId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Project id cannot be null");
        }
        ProjectInfo projectInfo = projectInfoService.getById(projectId);
        if (projectInfo == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return projectInfo;
    }

    private boolean detectGitRepository(ProjectInfo projectInfo) {
        if (Boolean.TRUE.equals(projectInfo.getHasGit())) {
            return true;
        }
        if (projectInfo.getProjectPath() == null || projectInfo.getProjectPath().isBlank()) {
            return false;
        }
        Path gitPath = Paths.get(projectInfo.getProjectPath()).resolve(".git");
        return Files.exists(gitPath) && Files.isDirectory(gitPath);
    }

    private int detectChangedFileCount(ProjectInfo projectInfo) {
        try {
            Path projectPath = Paths.get(projectInfo.getProjectPath());
            GitCommandExecutor.CommandResult result = gitCommandExecutor.execute(projectPath, List.of("git", "status", "--porcelain"));
            String stdout = result.getStdout();
            if (stdout == null || stdout.isBlank()) {
                return 0;
            }
            int count = 0;
            for (String line : stdout.split("\\R")) {
                if (line != null && !line.isBlank()) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            log.warn("Failed to detect git changed file count: {}", e.getMessage());
            return 0;
        }
    }

    private boolean checkRuleFileOnDisk(ProjectInfo projectInfo, AgentType agentType) {
        if (projectInfo.getProjectPath() == null || projectInfo.getProjectPath().isBlank()) {
            return false;
        }
        String fileName = switch (agentType) {
            case CODEX -> "AGENTS.md";
            case CLAUDE -> "CLAUDE.md";
            case CURSOR -> ".cursor/rules/agentguard.mdc";
        };
        Path rulePath = Paths.get(projectInfo.getProjectPath()).resolve(fileName);
        return Files.exists(rulePath) && Files.isRegularFile(rulePath);
    }

    private RiskReport saveReport(PreflightCheckRequest request, PreflightCheckResult result) {
        RiskReport report = new RiskReport();
        report.setProjectId(request.getProjectId());
        report.setReportType(REPORT_TYPE_PREFLIGHT_CHECK);
        report.setRiskLevel(result.getOverallRiskLevel().name());
        report.setRiskScore(result.getScore());
        report.setSummary("Agent 执行前预检完成，风险等级为 " + result.getOverallRiskLevel().name()
                + "，风险分数为 " + result.getScore());
        report.setRiskItems(JsonUtils.toJson(result.getRiskItems()));
        report.setSuggestions(JsonUtils.toJson(result.getSuggestions()));
        report.setPayloadJson(JsonUtils.toJson(PreflightReportPayload.from(request, result)));
        report.setCreatedTime(LocalDateTime.now());
        riskReportService.save(report);
        return report;
    }

    private PreflightCheckVO toVO(PreflightCheckRequest request, PreflightCheckResult result, LocalDateTime createdTime) {
        PreflightCheckVO vo = new PreflightCheckVO();
        vo.setProjectId(request.getProjectId());
        vo.setAgentType(request.getAgentType());
        vo.setTaskType(request.getTaskType());
        vo.setOverallRiskLevel(result.getOverallRiskLevel().name());
        vo.setScore(result.getScore());
        vo.setAllowedToProceed(result.getAllowedToProceed());
        vo.setCheckItems(result.getCheckItems());
        vo.setRiskItems(result.getRiskItems());
        vo.setSuggestions(result.getSuggestions());
        vo.setRecommendedActions(result.getRecommendedActions());
        vo.setCreatedTime(createdTime);
        return vo;
    }

    private PreflightCheckVO parseReport(RiskReport report) {
        PreflightCheckVO vo = new PreflightCheckVO();
        vo.setProjectId(report.getProjectId());
        vo.setOverallRiskLevel(report.getRiskLevel());
        vo.setCreatedTime(report.getCreatedTime());

        List<String> riskItems = JsonUtils.parseStringList(report.getRiskItems());
        vo.setRiskItems(riskItems);

        PreflightReportPayload payload = parsePayload(report);
        vo.setAgentType(payload.getAgentType());
        vo.setTaskType(payload.getTaskType());
        vo.setCheckItems(payload.getCheckItems() == null ? List.of() : payload.getCheckItems());
        vo.setAllowedToProceed(payload.getAllowedToProceed() == null
                ? (!"HIGH".equals(report.getRiskLevel()) && !"CRITICAL".equals(report.getRiskLevel()))
                : payload.getAllowedToProceed());
        vo.setScore(payload.getScore() == null
                ? (report.getRiskScore() == null ? estimateScore(riskItems) : report.getRiskScore())
                : payload.getScore());
        if (payload.getSuggestions() != null || payload.getRecommendedActions() != null) {
            vo.setSuggestions(payload.getSuggestions() == null ? List.of() : payload.getSuggestions());
            vo.setRecommendedActions(payload.getRecommendedActions() == null ? List.of() : payload.getRecommendedActions());
        } else {
            parseLegacySuggestions(report, vo);
        }

        return vo;
    }

    private PreflightReportPayload parsePayload(RiskReport report) {
        String json = report.getPayloadJson();
        if (json == null || json.isBlank() || !json.trim().startsWith("{")) {
            return new PreflightReportPayload();
        }
        PreflightReportPayload payload = JsonUtils.parseObject(json, PreflightReportPayload.class);
        return payload == null ? new PreflightReportPayload() : payload;
    }

    private void parseLegacySuggestions(RiskReport report, PreflightCheckVO vo) {
        List<String> allSuggestions = JsonUtils.parseStringList(report.getSuggestions());
        List<String> suggestions = new ArrayList<>();
        List<String> recommendedActions = new ArrayList<>();
        boolean inActions = false;
        for (String item : allSuggestions) {
            if ("建议操作：".equals(item)) {
                inActions = true;
                continue;
            }
            if (inActions) {
                recommendedActions.add(item);
            } else {
                suggestions.add(item);
            }
        }
        vo.setSuggestions(suggestions);
        vo.setRecommendedActions(recommendedActions);
    }

    private int estimateScore(List<String> riskItems) {
        if (riskItems == null || riskItems.isEmpty()) {
            return 0;
        }
        return Math.min(riskItems.size() * 15, 100);
    }

    public static class PreflightReportPayload {

        private String agentType;
        private String taskType;
        private String sandboxMode;
        private String approvalPolicy;
        private Boolean networkAccess;
        private Boolean allowDelete;
        private List<String> plannedCommands;
        private Integer score;
        private Boolean allowedToProceed;
        private List<PreflightCheckItemVO> checkItems;
        private List<String> suggestions;
        private List<String> recommendedActions;

        static PreflightReportPayload from(PreflightCheckRequest request, PreflightCheckResult result) {
            PreflightReportPayload payload = new PreflightReportPayload();
            payload.setAgentType(request.getAgentType());
            payload.setTaskType(request.getTaskType());
            payload.setSandboxMode(request.getSandboxMode());
            payload.setApprovalPolicy(request.getApprovalPolicy());
            payload.setNetworkAccess(request.getNetworkAccess());
            payload.setAllowDelete(request.getAllowDelete());
            payload.setPlannedCommands(request.getPlannedCommands());
            payload.setScore(result.getScore());
            payload.setAllowedToProceed(result.getAllowedToProceed());
            payload.setCheckItems(result.getCheckItems());
            payload.setSuggestions(result.getSuggestions());
            payload.setRecommendedActions(result.getRecommendedActions());
            return payload;
        }

        public String getAgentType() {
            return agentType;
        }

        public void setAgentType(String agentType) {
            this.agentType = agentType;
        }

        public String getTaskType() {
            return taskType;
        }

        public void setTaskType(String taskType) {
            this.taskType = taskType;
        }

        public String getSandboxMode() {
            return sandboxMode;
        }

        public void setSandboxMode(String sandboxMode) {
            this.sandboxMode = sandboxMode;
        }

        public String getApprovalPolicy() {
            return approvalPolicy;
        }

        public void setApprovalPolicy(String approvalPolicy) {
            this.approvalPolicy = approvalPolicy;
        }

        public Boolean getNetworkAccess() {
            return networkAccess;
        }

        public void setNetworkAccess(Boolean networkAccess) {
            this.networkAccess = networkAccess;
        }

        public Boolean getAllowDelete() {
            return allowDelete;
        }

        public void setAllowDelete(Boolean allowDelete) {
            this.allowDelete = allowDelete;
        }

        public List<String> getPlannedCommands() {
            return plannedCommands;
        }

        public void setPlannedCommands(List<String> plannedCommands) {
            this.plannedCommands = plannedCommands;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public Boolean getAllowedToProceed() {
            return allowedToProceed;
        }

        public void setAllowedToProceed(Boolean allowedToProceed) {
            this.allowedToProceed = allowedToProceed;
        }

        public List<PreflightCheckItemVO> getCheckItems() {
            return checkItems;
        }

        public void setCheckItems(List<PreflightCheckItemVO> checkItems) {
            this.checkItems = checkItems;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }

        public List<String> getRecommendedActions() {
            return recommendedActions;
        }

        public void setRecommendedActions(List<String> recommendedActions) {
            this.recommendedActions = recommendedActions;
        }
    }
}

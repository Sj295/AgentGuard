package com.agentguard.service.impl;

import com.agentguard.audit.GitDiffAuditor;
import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.common.JsonUtils;
import com.agentguard.common.enums.RiskReportType;
import com.agentguard.dto.GitDiffAuditRequest;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.RiskReport;
import com.agentguard.service.GitAuditService;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.RiskReportService;
import com.agentguard.vo.GitAuditReportDetailVO;
import com.agentguard.vo.GitDiffAuditVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GitAuditServiceImpl implements GitAuditService {

    private static final String REPORT_TYPE_GIT_DIFF_AUDIT = RiskReportType.GIT_DIFF_AUDIT.name();

    private final ProjectInfoService projectInfoService;
    private final RiskReportService riskReportService;
    private final GitDiffAuditor gitDiffAuditor;

    public GitAuditServiceImpl(ProjectInfoService projectInfoService,
                               RiskReportService riskReportService,
                               GitDiffAuditor gitDiffAuditor) {
        this.projectInfoService = projectInfoService;
        this.riskReportService = riskReportService;
        this.gitDiffAuditor = gitDiffAuditor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GitDiffAuditVO auditDiff(GitDiffAuditRequest request) {
        ProjectInfo projectInfo = loadProjectInfo(request.getProjectId());
        if (!Boolean.TRUE.equals(projectInfo.getHasGit())) {
            throw new BusinessException(ErrorCode.GIT_REPOSITORY_NOT_FOUND);
        }

        List<String> techStack = JsonUtils.parseStringList(projectInfo.getTechStack());
        Path projectPath = Paths.get(projectInfo.getProjectPath());
        GitDiffAuditor.GitDiffAuditResult auditResult = gitDiffAuditor.audit(projectPath, techStack);

        RiskReport riskReport = saveAuditReport(request.getProjectId(), auditResult);
        return toAuditVO(riskReport, auditResult);
    }

    @Override
    public List<GitDiffAuditVO> listProjectReports(Long projectId) {
        loadProjectInfo(projectId);
        List<RiskReport> reports = riskReportService.lambdaQuery()
                .eq(RiskReport::getProjectId, projectId)
                .eq(RiskReport::getReportType, REPORT_TYPE_GIT_DIFF_AUDIT)
                .orderByDesc(RiskReport::getCreatedTime)
                .orderByDesc(RiskReport::getId)
                .list();

        List<GitDiffAuditVO> result = new ArrayList<>();
        for (RiskReport report : reports) {
            result.add(parseReport(report));
        }
        return result;
    }

    @Override
    public GitAuditReportDetailVO getReportDetail(Long reportId) {
        if (reportId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Report id cannot be null");
        }
        RiskReport report = riskReportService.getById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        if (!RiskReportType.GIT_DIFF_AUDIT.name().equals(report.getReportType())) {
            throw new BusinessException(ErrorCode.INVALID_REPORT_TYPE, "Report type is not GIT_DIFF_AUDIT");
        }
        return toGitAuditReportDetailVO(report);
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

    private RiskReport saveAuditReport(Long projectId, GitDiffAuditor.GitDiffAuditResult auditResult) {
        RiskReport report = new RiskReport();
        report.setProjectId(projectId);
        report.setReportType(REPORT_TYPE_GIT_DIFF_AUDIT);
        report.setRiskLevel(auditResult.getRiskLevel().name());
        report.setRiskScore(estimateScore(auditResult));
        report.setSummary("Git Diff 审计完成，变更文件 " + auditResult.getChangedFileCount()
                + " 个，风险等级为 " + auditResult.getRiskLevel().name());
        report.setRiskItems(toJson(auditResult.getRiskItems()));
        report.setSuggestions(toJson(auditResult.getSuggestions()));
        report.setPayloadJson(toJson(buildPayload(auditResult)));
        report.setCreatedTime(LocalDateTime.now());
        riskReportService.save(report);
        return report;
    }

    private GitDiffAuditVO toAuditVO(RiskReport report, GitDiffAuditor.GitDiffAuditResult auditResult) {
        GitDiffAuditVO vo = new GitDiffAuditVO();
        vo.setReportId(report.getId());
        vo.setProjectId(report.getProjectId());
        vo.setChangedFileCount(auditResult.getChangedFileCount());
        vo.setAddedFiles(auditResult.getAddedFiles());
        vo.setModifiedFiles(auditResult.getModifiedFiles());
        vo.setDeletedFiles(auditResult.getDeletedFiles());
        vo.setRiskLevel(auditResult.getRiskLevel().name());
        vo.setRiskItems(auditResult.getRiskItems());
        vo.setSuggestions(auditResult.getSuggestions());
        vo.setRollbackCommands(auditResult.getRollbackCommands());
        vo.setCreatedTime(report.getCreatedTime());
        return vo;
    }

    private GitDiffAuditVO parseReport(RiskReport report) {
        GitDiffAuditVO vo = new GitDiffAuditVO();
        vo.setReportId(report.getId());
        vo.setProjectId(report.getProjectId());
        vo.setRiskLevel(report.getRiskLevel());
        vo.setRiskItems(JsonUtils.parseStringList(report.getRiskItems()));
        vo.setCreatedTime(report.getCreatedTime());

        GitAuditPayload payload = parseStructuredPayload(report);
        vo.setChangedFileCount(payload.getChangedFileCount() == null ? 0 : payload.getChangedFileCount());
        vo.setAddedFiles(payload.getAddedFiles() == null ? List.of() : payload.getAddedFiles());
        vo.setModifiedFiles(payload.getModifiedFiles() == null ? List.of() : payload.getModifiedFiles());
        vo.setDeletedFiles(payload.getDeletedFiles() == null ? List.of() : payload.getDeletedFiles());
        vo.setSuggestions(payload.getSuggestions() == null ? JsonUtils.parseStringList(report.getSuggestions()) : payload.getSuggestions());
        vo.setRollbackCommands(payload.getRollbackCommands() == null ? List.of() : payload.getRollbackCommands());
        return vo;
    }

    private GitAuditReportDetailVO toGitAuditReportDetailVO(RiskReport report) {
        GitAuditReportDetailVO vo = new GitAuditReportDetailVO();
        vo.setId(report.getId());
        vo.setProjectId(report.getProjectId());
        vo.setReportType(report.getReportType());
        vo.setRiskLevel(report.getRiskLevel());
        vo.setRiskScore(report.getRiskScore());
        vo.setSummary(report.getSummary());
        vo.setRiskItems(JsonUtils.parseStringList(report.getRiskItems()));
        GitAuditPayload payload = parseStructuredPayload(report);
        vo.setAddedFiles(payload.getAddedFiles() == null ? List.of() : payload.getAddedFiles());
        vo.setModifiedFiles(payload.getModifiedFiles() == null ? List.of() : payload.getModifiedFiles());
        vo.setDeletedFiles(payload.getDeletedFiles() == null ? List.of() : payload.getDeletedFiles());
        vo.setRollbackCommands(payload.getRollbackCommands() == null ? List.of() : payload.getRollbackCommands());
        vo.setSuggestions(payload.getSuggestions() == null ? JsonUtils.parseStringList(report.getSuggestions()) : payload.getSuggestions());
        vo.setPayloadJson(report.getPayloadJson());
        vo.setCreatedTime(report.getCreatedTime());
        return vo;
    }

    private GitAuditPayload buildPayload(GitDiffAuditor.GitDiffAuditResult auditResult) {
        GitAuditPayload payload = new GitAuditPayload();
        payload.setChangedFileCount(auditResult.getChangedFileCount());
        payload.setScore(estimateScore(auditResult));
        payload.setAddedFiles(auditResult.getAddedFiles());
        payload.setModifiedFiles(auditResult.getModifiedFiles());
        payload.setDeletedFiles(auditResult.getDeletedFiles());
        payload.setSuggestions(auditResult.getSuggestions());
        payload.setRollbackCommands(auditResult.getRollbackCommands());
        return payload;
    }

    private GitAuditPayload parseStructuredPayload(RiskReport report) {
        GitAuditPayload payload = parsePayload(report.getPayloadJson());
        if (hasPayloadData(payload)) {
            return payload;
        }
        return parsePayload(report.getSuggestions());
    }

    private boolean hasPayloadData(GitAuditPayload payload) {
        if (payload == null) {
            return false;
        }
        return payload.getChangedFileCount() != null
                || payload.getScore() != null
                || !safeList(payload.getAddedFiles()).isEmpty()
                || !safeList(payload.getModifiedFiles()).isEmpty()
                || !safeList(payload.getDeletedFiles()).isEmpty()
                || !safeList(payload.getSuggestions()).isEmpty()
                || !safeList(payload.getRollbackCommands()).isEmpty();
    }

    private GitAuditPayload parsePayload(String json) {
        if (json == null || json.isBlank() || !json.trim().startsWith("{")) {
            return new GitAuditPayload();
        }
        Map<String, Object> payloadMap = JsonUtils.parseMap(json);
        GitAuditPayload payload = new GitAuditPayload();
        payload.setChangedFileCount(toInteger(payloadMap.get("changedFileCount")));
        payload.setScore(toInteger(payloadMap.get("score")));
        payload.setAddedFiles(toStringList(payloadMap.get("addedFiles")));
        payload.setModifiedFiles(toStringList(payloadMap.get("modifiedFiles")));
        payload.setDeletedFiles(toStringList(payloadMap.get("deletedFiles")));
        payload.setSuggestions(toStringList(payloadMap.get("suggestions")));
        payload.setRollbackCommands(toStringList(payloadMap.get("rollbackCommands")));
        return payload;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String toJson(Object value) {
        return JsonUtils.toJson(value);
    }

    private int estimateScore(GitDiffAuditor.GitDiffAuditResult auditResult) {
        int score = switch (auditResult.getRiskLevel()) {
            case LOW -> 10;
            case MEDIUM -> 45;
            case HIGH -> 75;
            case CRITICAL -> 95;
        };
        score += Math.min(auditResult.getChangedFileCount(), 20);
        if (auditResult.getRiskItems() != null) {
            score += Math.min(auditResult.getRiskItems().size() * 3, 15);
        }
        return Math.min(score, 100);
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> values = new ArrayList<>();
            for (Object item : list) {
                values.add(item == null ? "" : item.toString());
            }
            return values;
        }
        if (value instanceof String text) {
            return List.of(text);
        }
        return List.of();
    }

    public static class GitAuditPayload {

        private Integer changedFileCount;
        private Integer score;
        private List<String> addedFiles;
        private List<String> modifiedFiles;
        private List<String> deletedFiles;
        private List<String> suggestions;
        private List<String> rollbackCommands;

        public Integer getChangedFileCount() {
            return changedFileCount;
        }

        public void setChangedFileCount(Integer changedFileCount) {
            this.changedFileCount = changedFileCount;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public List<String> getAddedFiles() {
            return addedFiles;
        }

        public void setAddedFiles(List<String> addedFiles) {
            this.addedFiles = addedFiles;
        }

        public List<String> getModifiedFiles() {
            return modifiedFiles;
        }

        public void setModifiedFiles(List<String> modifiedFiles) {
            this.modifiedFiles = modifiedFiles;
        }

        public List<String> getDeletedFiles() {
            return deletedFiles;
        }

        public void setDeletedFiles(List<String> deletedFiles) {
            this.deletedFiles = deletedFiles;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }

        public List<String> getRollbackCommands() {
            return rollbackCommands;
        }

        public void setRollbackCommands(List<String> rollbackCommands) {
            this.rollbackCommands = rollbackCommands;
        }
    }
}

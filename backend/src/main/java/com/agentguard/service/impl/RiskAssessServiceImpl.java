package com.agentguard.service.impl;

import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.common.JsonUtils;
import com.agentguard.common.PageResult;
import com.agentguard.common.enums.AgentType;
import com.agentguard.common.enums.ApprovalPolicy;
import com.agentguard.common.enums.RiskLevel;
import com.agentguard.common.enums.RiskReportType;
import com.agentguard.common.enums.SandboxMode;
import com.agentguard.common.enums.TaskType;
import com.agentguard.dto.PermissionAssessRequest;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.RiskReport;
import com.agentguard.entity.ScanResult;
import com.agentguard.risk.PermissionAssessContext;
import com.agentguard.risk.PermissionAssessResult;
import com.agentguard.risk.PermissionRiskAssessor;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.RiskAssessService;
import com.agentguard.service.RiskReportService;
import com.agentguard.service.ScanResultService;
import com.agentguard.vo.PermissionAssessVO;
import com.agentguard.vo.RiskReportDetailVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

@Service
public class RiskAssessServiceImpl implements RiskAssessService {

    private static final String REPORT_TYPE_PERMISSION_ASSESS = RiskReportType.PERMISSION_ASSESS.name();

    private final ProjectInfoService projectInfoService;
    private final ScanResultService scanResultService;
    private final RiskReportService riskReportService;
    private final PermissionRiskAssessor permissionRiskAssessor;
    private final ObjectMapper objectMapper;

    public RiskAssessServiceImpl(ProjectInfoService projectInfoService,
                                 ScanResultService scanResultService,
                                 RiskReportService riskReportService,
                                 PermissionRiskAssessor permissionRiskAssessor,
                                 ObjectMapper objectMapper) {
        this.projectInfoService = projectInfoService;
        this.scanResultService = scanResultService;
        this.riskReportService = riskReportService;
        this.permissionRiskAssessor = permissionRiskAssessor;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionAssessVO assessPermission(PermissionAssessRequest request) {
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

        RiskLevel latestScanRiskLevel = null;
        List<String> latestSensitiveFiles = List.of();
        boolean hasScanResult = latestScanResult != null;
        if (hasScanResult) {
            latestScanRiskLevel = RiskLevel.fromCode(latestScanResult.getRiskLevel());
            latestSensitiveFiles = JsonUtils.parseStringList(latestScanResult.getSensitiveFiles());
        }

        PermissionAssessContext context = PermissionAssessContext.builder()
                .projectId(request.getProjectId())
                .agentType(agentType)
                .taskType(taskType)
                .sandboxMode(sandboxMode)
                .approvalPolicy(approvalPolicy)
                .networkAccess(Boolean.TRUE.equals(request.getNetworkAccess()))
                .allowDelete(Boolean.TRUE.equals(request.getAllowDelete()))
                .projectInfo(projectInfo)
                .latestScanResult(latestScanResult)
                .latestScanRiskLevel(latestScanRiskLevel)
                .latestSensitiveFiles(latestSensitiveFiles)
                .hasScanResult(hasScanResult)
                .build();

        PermissionAssessResult assessResult = permissionRiskAssessor.assess(context);
        RiskReport riskReport = saveRiskReport(context, assessResult);
        return buildPermissionAssessVO(riskReport, context, assessResult);
    }

    @Override
    public List<PermissionAssessVO> listProjectReports(Long projectId) {
        loadProjectInfo(projectId);
        List<RiskReport> reports = riskReportService.lambdaQuery()
                .eq(RiskReport::getProjectId, projectId)
                .eq(RiskReport::getReportType, REPORT_TYPE_PERMISSION_ASSESS)
                .orderByDesc(RiskReport::getCreatedTime)
                .orderByDesc(RiskReport::getId)
                .list();
        List<PermissionAssessVO> result = new ArrayList<>();
        for (RiskReport report : reports) {
            result.add(parsePermissionAssessReport(report));
        }
        return result;
    }

    @Override
    public RiskReportDetailVO getReportDetail(Long reportId) {
        if (reportId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Report id cannot be null");
        }
        RiskReport report = riskReportService.getById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        return toRiskReportDetailVO(report);
    }

    @Override
    public PageResult<RiskReportDetailVO> pageProjectReportsByType(Long projectId, String reportType, long current, long size) {
        loadProjectInfo(projectId);
        RiskReportType parsedType = RiskReportType.fromCode(reportType);
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Pagination parameters must be greater than 0");
        }
        Page<RiskReport> page = riskReportService.page(
                new Page<>(current, size),
                Wrappers.<RiskReport>lambdaQuery()
                        .eq(RiskReport::getProjectId, projectId)
                        .eq(RiskReport::getReportType, parsedType.name())
                        .orderByDesc(RiskReport::getCreatedTime)
                        .orderByDesc(RiskReport::getId)
        );
        Page<RiskReportDetailVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::toRiskReportDetailVO).toList());
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

    private RiskReport saveRiskReport(PermissionAssessContext context, PermissionAssessResult assessResult) {
        RiskReport riskReport = new RiskReport();
        riskReport.setProjectId(context.getProjectId());
        riskReport.setReportType(REPORT_TYPE_PERMISSION_ASSESS);
        riskReport.setRiskLevel(assessResult.getRiskLevel().name());
        riskReport.setRiskScore(assessResult.getScore());
        riskReport.setSummary("权限风险评估完成，风险等级为 " + assessResult.getRiskLevel().name()
                + "，风险分数为 " + assessResult.getScore());
        riskReport.setRiskItems(toJson(assessResult.getRiskItems()));
        riskReport.setSuggestions(toJson(assessResult.getSuggestions()));
        riskReport.setPayloadJson(toJson(buildStoredPayload(context, assessResult)));
        riskReport.setCreatedTime(LocalDateTime.now());
        riskReportService.save(riskReport);
        return riskReport;
    }

    private PermissionAssessVO buildPermissionAssessVO(RiskReport report,
                                                       PermissionAssessContext context,
                                                       PermissionAssessResult assessResult) {
        PermissionAssessVO vo = new PermissionAssessVO();
        vo.setReportId(report.getId());
        vo.setProjectId(context.getProjectId());
        vo.setAgentType(context.getAgentType().name());
        vo.setTaskType(context.getTaskType().name());
        vo.setSandboxMode(context.getSandboxMode().name());
        vo.setApprovalPolicy(context.getApprovalPolicy().name());
        vo.setNetworkAccess(context.isNetworkAccess());
        vo.setAllowDelete(context.isAllowDelete());
        vo.setRiskLevel(assessResult.getRiskLevel().name());
        vo.setScore(assessResult.getScore());
        vo.setRiskItems(assessResult.getRiskItems());
        vo.setSuggestions(assessResult.getSuggestions());
        vo.setRecommendedConfig(assessResult.getRecommendedConfig());
        vo.setCreatedTime(report.getCreatedTime());
        return vo;
    }

    private PermissionAssessVO parsePermissionAssessReport(RiskReport report) {
        PermissionAssessVO vo = new PermissionAssessVO();
        vo.setReportId(report.getId());
        vo.setProjectId(report.getProjectId());
        vo.setRiskLevel(report.getRiskLevel());
        vo.setRiskItems(JsonUtils.parseStringList(report.getRiskItems()));
        vo.setCreatedTime(report.getCreatedTime());

        StoredRiskPayload payload = parseStoredPayload(report);
        vo.setAgentType(payload.getAgentType());
        vo.setTaskType(payload.getTaskType());
        vo.setSandboxMode(payload.getSandboxMode());
        vo.setApprovalPolicy(payload.getApprovalPolicy());
        vo.setNetworkAccess(payload.getNetworkAccess());
        vo.setAllowDelete(payload.getAllowDelete());
        vo.setScore(payload.getScore() == null ? report.getRiskScore() : payload.getScore());
        vo.setSuggestions(payload.getSuggestions() == null ? JsonUtils.parseStringList(report.getSuggestions()) : payload.getSuggestions());
        vo.setRecommendedConfig(payload.getRecommendedConfig() == null ? Map.of() : payload.getRecommendedConfig());
        return vo;
    }

    private StoredRiskPayload buildStoredPayload(PermissionAssessContext context, PermissionAssessResult assessResult) {
        StoredRiskPayload payload = new StoredRiskPayload();
        payload.setAgentType(context.getAgentType().name());
        payload.setTaskType(context.getTaskType().name());
        payload.setSandboxMode(context.getSandboxMode().name());
        payload.setApprovalPolicy(context.getApprovalPolicy().name());
        payload.setNetworkAccess(context.isNetworkAccess());
        payload.setAllowDelete(context.isAllowDelete());
        payload.setScore(assessResult.getScore());
        payload.setSuggestions(assessResult.getSuggestions());
        payload.setRecommendedConfig(new LinkedHashMap<>(assessResult.getRecommendedConfig()));
        return payload;
    }

    private StoredRiskPayload parseStoredPayload(RiskReport report) {
        String json = report.getPayloadJson();
        if (json == null || json.isBlank()) {
            json = report.getSuggestions();
        }
        if (json == null || json.isBlank()) {
            return new StoredRiskPayload();
        }
        if (!json.trim().startsWith("{")) {
            return new StoredRiskPayload();
        }
        try {
            return objectMapper.readValue(json, StoredRiskPayload.class);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to parse stored risk payload", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to serialize JSON value", exception);
        }
    }

    private RiskReportDetailVO toRiskReportDetailVO(RiskReport report) {
        RiskReportDetailVO vo = new RiskReportDetailVO();
        vo.setId(report.getId());
        vo.setProjectId(report.getProjectId());
        vo.setReportType(report.getReportType());
        vo.setRiskLevel(report.getRiskLevel());
        vo.setRiskScore(report.getRiskScore());
        vo.setSummary(report.getSummary());
        vo.setRiskItems(JsonUtils.parseStringList(report.getRiskItems()));
        vo.setSuggestions(JsonUtils.parseStringList(report.getSuggestions()));
        vo.setPayloadJson(report.getPayloadJson());
        vo.setCreatedTime(report.getCreatedTime());
        return vo;
    }

    public static class StoredRiskPayload {

        private String agentType;
        private String taskType;
        private String sandboxMode;
        private String approvalPolicy;
        private Boolean networkAccess;
        private Boolean allowDelete;
        private Integer score;
        private List<String> suggestions;
        private Map<String, Object> recommendedConfig;

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

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void setSuggestions(List<String> suggestions) {
            this.suggestions = suggestions;
        }

        public Map<String, Object> getRecommendedConfig() {
            return recommendedConfig;
        }

        public void setRecommendedConfig(Map<String, Object> recommendedConfig) {
            this.recommendedConfig = recommendedConfig;
        }
    }
}

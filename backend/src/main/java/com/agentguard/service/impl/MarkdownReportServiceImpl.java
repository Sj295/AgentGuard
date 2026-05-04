package com.agentguard.service.impl;

import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.common.enums.RiskReportType;
import com.agentguard.dto.MarkdownReportExportRequest;
import com.agentguard.dto.MarkdownReportGenerateRequest;
import com.agentguard.entity.AgentRule;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.RiskReport;
import com.agentguard.entity.ScanResult;
import com.agentguard.report.MarkdownReportContext;
import com.agentguard.report.MarkdownSecurityReportGenerator;
import com.agentguard.service.AgentRuleService;
import com.agentguard.service.MarkdownReportService;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.RiskReportService;
import com.agentguard.service.ScanResultService;
import com.agentguard.vo.MarkdownReportVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MarkdownReportServiceImpl implements MarkdownReportService {

    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final ProjectInfoService projectInfoService;
    private final ScanResultService scanResultService;
    private final AgentRuleService agentRuleService;
    private final RiskReportService riskReportService;
    private final MarkdownSecurityReportGenerator reportGenerator;

    public MarkdownReportServiceImpl(ProjectInfoService projectInfoService,
                                     ScanResultService scanResultService,
                                     AgentRuleService agentRuleService,
                                     RiskReportService riskReportService,
                                     MarkdownSecurityReportGenerator reportGenerator) {
        this.projectInfoService = projectInfoService;
        this.scanResultService = scanResultService;
        this.agentRuleService = agentRuleService;
        this.riskReportService = riskReportService;
        this.reportGenerator = reportGenerator;
    }

    @Override
    public MarkdownReportVO generate(MarkdownReportGenerateRequest request) {
        ProjectInfo projectInfo = loadProjectInfo(request.getProjectId());
        MarkdownReportContext context = buildContext(projectInfo);

        String markdown = reportGenerator.generate(
                context,
                Boolean.TRUE.equals(request.getIncludeScanResult()),
                Boolean.TRUE.equals(request.getIncludeAgentRules()),
                Boolean.TRUE.equals(request.getIncludeRiskReports()),
                Boolean.TRUE.equals(request.getIncludeGitAudit()),
                Boolean.TRUE.equals(request.getIncludePreflight())
        );

        String fileName = buildFileName(projectInfo.getProjectName());

        MarkdownReportVO vo = new MarkdownReportVO();
        vo.setProjectId(projectInfo.getId());
        vo.setProjectName(projectInfo.getProjectName());
        vo.setFileName(fileName);
        vo.setMarkdown(markdown);
        vo.setWritten(false);
        vo.setCreatedTime(LocalDateTime.now());
        return vo;
    }

    @Override
    public MarkdownReportVO export(MarkdownReportExportRequest request) {
        ProjectInfo projectInfo = loadProjectInfo(request.getProjectId());
        MarkdownReportContext context = buildContext(projectInfo);

        String markdown = reportGenerator.generate(
                context,
                Boolean.TRUE.equals(request.getIncludeScanResult()),
                Boolean.TRUE.equals(request.getIncludeAgentRules()),
                Boolean.TRUE.equals(request.getIncludeRiskReports()),
                Boolean.TRUE.equals(request.getIncludeGitAudit()),
                Boolean.TRUE.equals(request.getIncludePreflight())
        );

        String fileName = buildFileName(projectInfo.getProjectName());
        Path projectRoot = validateProjectPath(projectInfo.getProjectPath());
        Path reportsDir = projectRoot.resolve(".agentguard").resolve("reports");
        Path targetPath = reportsDir.resolve(fileName);

        validateTargetInsideProject(targetPath, projectRoot);

        if (Files.exists(targetPath) && !Boolean.TRUE.equals(request.getOverwrite())) {
            throw new BusinessException(ErrorCode.FILE_ALREADY_EXISTS);
        }

        try {
            Files.createDirectories(reportsDir);
            Files.writeString(targetPath, markdown, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("写入报告文件失败: " + targetPath, e);
        }

        MarkdownReportVO vo = new MarkdownReportVO();
        vo.setProjectId(projectInfo.getId());
        vo.setProjectName(projectInfo.getProjectName());
        vo.setFileName(fileName);
        vo.setTargetPath(targetPath.toString().replace('\\', '/'));
        vo.setWritten(true);
        vo.setMarkdown(markdown);
        vo.setCreatedTime(LocalDateTime.now());
        return vo;
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

    private MarkdownReportContext buildContext(ProjectInfo projectInfo) {
        Long projectId = projectInfo.getId();

        ScanResult latestScanResult = scanResultService.lambdaQuery()
                .eq(ScanResult::getProjectId, projectId)
                .orderByDesc(ScanResult::getCreatedTime)
                .orderByDesc(ScanResult::getId)
                .last("limit 1")
                .one();

        List<AgentRule> agentRules = agentRuleService.lambdaQuery()
                .eq(AgentRule::getProjectId, projectId)
                .orderByDesc(AgentRule::getUpdatedTime)
                .orderByDesc(AgentRule::getCreatedTime)
                .list();

        List<RiskReport> latestRiskReports = riskReportService.lambdaQuery()
                .eq(RiskReport::getProjectId, projectId)
                .orderByDesc(RiskReport::getCreatedTime)
                .orderByDesc(RiskReport::getId)
                .last("limit 10")
                .list();

        RiskReport latestPermissionReport = riskReportService.lambdaQuery()
                .eq(RiskReport::getProjectId, projectId)
                .eq(RiskReport::getReportType, RiskReportType.PERMISSION_ASSESS.name())
                .orderByDesc(RiskReport::getCreatedTime)
                .orderByDesc(RiskReport::getId)
                .last("limit 1")
                .one();

        RiskReport latestCommandAuditReport = riskReportService.lambdaQuery()
                .eq(RiskReport::getProjectId, projectId)
                .eq(RiskReport::getReportType, RiskReportType.COMMAND_AUDIT.name())
                .orderByDesc(RiskReport::getCreatedTime)
                .orderByDesc(RiskReport::getId)
                .last("limit 1")
                .one();

        RiskReport latestGitAuditReport = riskReportService.lambdaQuery()
                .eq(RiskReport::getProjectId, projectId)
                .eq(RiskReport::getReportType, RiskReportType.GIT_DIFF_AUDIT.name())
                .orderByDesc(RiskReport::getCreatedTime)
                .orderByDesc(RiskReport::getId)
                .last("limit 1")
                .one();

        RiskReport latestPreflightReport = riskReportService.lambdaQuery()
                .eq(RiskReport::getProjectId, projectId)
                .eq(RiskReport::getReportType, RiskReportType.PREFLIGHT_CHECK.name())
                .orderByDesc(RiskReport::getCreatedTime)
                .orderByDesc(RiskReport::getId)
                .last("limit 1")
                .one();

        return MarkdownReportContext.builder()
                .projectInfo(projectInfo)
                .latestScanResult(latestScanResult)
                .agentRules(agentRules)
                .latestRiskReports(latestRiskReports)
                .latestPermissionReport(latestPermissionReport)
                .latestCommandAuditReport(latestCommandAuditReport)
                .latestGitAuditReport(latestGitAuditReport)
                .latestPreflightReport(latestPreflightReport)
                .build();
    }

    private String buildFileName(String projectName) {
        String safeName = projectName != null && !projectName.isBlank()
                ? projectName.replaceAll("[^a-zA-Z0-9\\-_]", "-")
                : "project";
        String timestamp = LocalDateTime.now().format(FILE_TIME_FORMAT);
        return safeName + "-security-report-" + timestamp + ".md";
    }

    private Path validateProjectPath(String projectPath) {
        if (projectPath == null || projectPath.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_PROJECT_PATH, "Project path cannot be blank");
        }
        Path path = Paths.get(projectPath);
        if (!Files.exists(path)) {
            throw new BusinessException(ErrorCode.INVALID_PROJECT_PATH, "Project path does not exist: " + projectPath);
        }
        if (!Files.isDirectory(path)) {
            throw new BusinessException(ErrorCode.INVALID_PROJECT_PATH, "Project path is not a directory: " + projectPath);
        }
        return path.toAbsolutePath().normalize();
    }

    private void validateTargetInsideProject(Path targetPath, Path projectRoot) {
        if (!targetPath.startsWith(projectRoot)) {
            throw new BusinessException(ErrorCode.INVALID_PROJECT_PATH, "Target path is outside project directory, write rejected");
        }
    }
}

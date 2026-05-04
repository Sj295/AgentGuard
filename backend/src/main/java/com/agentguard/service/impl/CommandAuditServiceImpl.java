package com.agentguard.service.impl;

import com.agentguard.command.CommandAuditContext;
import com.agentguard.command.CommandAuditResult;
import com.agentguard.command.CommandRiskAuditor;
import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.common.JsonUtils;
import com.agentguard.common.PageResult;
import com.agentguard.common.enums.RiskReportType;
import com.agentguard.dto.CommandAuditRequest;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.RiskReport;
import com.agentguard.entity.ScanResult;
import com.agentguard.service.CommandAuditService;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.RiskReportService;
import com.agentguard.service.ScanResultService;
import com.agentguard.vo.CommandAuditVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CommandAuditServiceImpl implements CommandAuditService {

    private static final String REPORT_TYPE_COMMAND_AUDIT = RiskReportType.COMMAND_AUDIT.name();

    private final ProjectInfoService projectInfoService;
    private final ScanResultService scanResultService;
    private final RiskReportService riskReportService;
    private final CommandRiskAuditor commandRiskAuditor;

    public CommandAuditServiceImpl(ProjectInfoService projectInfoService,
                                   ScanResultService scanResultService,
                                   RiskReportService riskReportService,
                                   CommandRiskAuditor commandRiskAuditor) {
        this.projectInfoService = projectInfoService;
        this.scanResultService = scanResultService;
        this.riskReportService = riskReportService;
        this.commandRiskAuditor = commandRiskAuditor;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommandAuditVO auditCommands(CommandAuditRequest request) {
        ProjectInfo projectInfo = loadProjectInfo(request.getProjectId());

        ScanResult latestScanResult = scanResultService.lambdaQuery()
                .eq(ScanResult::getProjectId, request.getProjectId())
                .orderByDesc(ScanResult::getCreatedTime)
                .orderByDesc(ScanResult::getId)
                .last("limit 1")
                .one();

        CommandAuditContext context = CommandAuditContext.builder()
                .projectId(request.getProjectId())
                .projectInfo(projectInfo)
                .latestScanResult(latestScanResult)
                .commands(request.getCommands())
                .hasScanResult(latestScanResult != null)
                .build();

        CommandAuditResult auditResult = commandRiskAuditor.audit(context);
        RiskReport report = saveReport(request.getProjectId(), auditResult);
        return toVO(report, request.getProjectId(), auditResult);
    }

    @Override
    public PageResult<CommandAuditVO> pageProjectReports(Long projectId, long current, long size) {
        loadProjectInfo(projectId);
        if (current <= 0 || size <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Pagination parameters must be greater than 0");
        }
        Page<RiskReport> page = riskReportService.page(
                new Page<>(current, size),
                Wrappers.<RiskReport>lambdaQuery()
                        .eq(RiskReport::getProjectId, projectId)
                        .eq(RiskReport::getReportType, REPORT_TYPE_COMMAND_AUDIT)
                        .orderByDesc(RiskReport::getCreatedTime)
                        .orderByDesc(RiskReport::getId)
        );
        Page<CommandAuditVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
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

    private RiskReport saveReport(Long projectId, CommandAuditResult result) {
        RiskReport report = new RiskReport();
        report.setProjectId(projectId);
        report.setReportType(REPORT_TYPE_COMMAND_AUDIT);
        report.setRiskLevel(result.getRiskLevel().name());
        report.setRiskScore(result.getScore());
        report.setSummary("命令审计完成，风险等级为 " + result.getRiskLevel().name()
                + "，风险分数为 " + result.getScore());
        report.setRiskItems(JsonUtils.toJson(result.getRiskItems()));
        report.setSuggestions(JsonUtils.toJson(result.getSuggestions()));
        report.setPayloadJson(JsonUtils.toJson(CommandAuditPayload.from(result)));
        report.setCreatedTime(LocalDateTime.now());
        riskReportService.save(report);
        return report;
    }

    private CommandAuditVO toVO(RiskReport report, Long projectId, CommandAuditResult result) {
        CommandAuditVO vo = new CommandAuditVO();
        vo.setReportId(report.getId());
        vo.setProjectId(projectId);
        vo.setRiskLevel(result.getRiskLevel().name());
        vo.setScore(result.getScore());
        vo.setRiskItems(result.getRiskItems());
        vo.setSuggestions(result.getSuggestions());
        vo.setSafeAlternatives(result.getSafeAlternatives());
        vo.setCreatedTime(report.getCreatedTime());
        return vo;
    }

    private CommandAuditVO parseReport(RiskReport report) {
        CommandAuditVO vo = new CommandAuditVO();
        vo.setReportId(report.getId());
        vo.setProjectId(report.getProjectId());
        vo.setRiskLevel(report.getRiskLevel());
        vo.setCreatedTime(report.getCreatedTime());

        List<String> riskItems = JsonUtils.parseStringList(report.getRiskItems());
        vo.setRiskItems(riskItems);
        CommandAuditPayload payload = parsePayload(report);
        vo.setScore(payload.getScore() == null
                ? (report.getRiskScore() == null ? estimateScore(riskItems) : report.getRiskScore())
                : payload.getScore());

        if (payload.getSuggestions() != null || payload.getSafeAlternatives() != null) {
            vo.setSuggestions(payload.getSuggestions() == null ? List.of() : payload.getSuggestions());
            vo.setSafeAlternatives(payload.getSafeAlternatives() == null ? List.of() : payload.getSafeAlternatives());
        } else {
            parseLegacySuggestions(report, vo);
        }
        return vo;
    }

    private CommandAuditPayload parsePayload(RiskReport report) {
        String json = report.getPayloadJson();
        if (json == null || json.isBlank() || !json.trim().startsWith("{")) {
            return new CommandAuditPayload();
        }
        CommandAuditPayload payload = JsonUtils.parseObject(json, CommandAuditPayload.class);
        return payload == null ? new CommandAuditPayload() : payload;
    }

    private void parseLegacySuggestions(RiskReport report, CommandAuditVO vo) {
        List<String> allSuggestions = JsonUtils.parseStringList(report.getSuggestions());
        List<String> suggestions = new ArrayList<>();
        List<String> safeAlternatives = new ArrayList<>();
        boolean inAlternatives = false;
        for (String item : allSuggestions) {
            if ("安全替代方案：".equals(item)) {
                inAlternatives = true;
                continue;
            }
            if (inAlternatives) {
                safeAlternatives.add(item);
            } else {
                suggestions.add(item);
            }
        }
        vo.setSuggestions(suggestions);
        vo.setSafeAlternatives(safeAlternatives);
    }

    private int estimateScore(List<String> riskItems) {
        if (riskItems == null || riskItems.isEmpty()) {
            return 0;
        }
        return Math.min(riskItems.size() * 15, 100);
    }

    public static class CommandAuditPayload {

        private Integer score;
        private List<String> suggestions;
        private List<String> safeAlternatives;

        static CommandAuditPayload from(CommandAuditResult result) {
            CommandAuditPayload payload = new CommandAuditPayload();
            payload.setScore(result.getScore());
            payload.setSuggestions(result.getSuggestions());
            payload.setSafeAlternatives(result.getSafeAlternatives());
            return payload;
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

        public List<String> getSafeAlternatives() {
            return safeAlternatives;
        }

        public void setSafeAlternatives(List<String> safeAlternatives) {
            this.safeAlternatives = safeAlternatives;
        }
    }
}

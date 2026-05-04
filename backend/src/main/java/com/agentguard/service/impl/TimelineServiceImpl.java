package com.agentguard.service.impl;

import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.common.PageResult;
import com.agentguard.common.enums.RiskLevel;
import com.agentguard.common.enums.RiskReportType;
import com.agentguard.entity.AgentRule;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.RiskReport;
import com.agentguard.entity.ScanResult;
import com.agentguard.service.AgentRuleService;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.RiskReportService;
import com.agentguard.service.ScanResultService;
import com.agentguard.service.TimelineService;
import com.agentguard.vo.ProjectSecurityOverviewVO;
import com.agentguard.vo.TimelineEventVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class TimelineServiceImpl implements TimelineService {

    private static final Map<String, String> REPORT_TYPE_EVENT_NAMES = Map.of(
            RiskReportType.PERMISSION_ASSESS.name(), "权限风险评估",
            RiskReportType.COMMAND_AUDIT.name(), "危险命令审计",
            RiskReportType.PREFLIGHT_CHECK.name(), "Agent 执行前预检",
            RiskReportType.GIT_DIFF_AUDIT.name(), "Git Diff 变更审计",
            RiskReportType.SENSITIVE_FILE_SCAN.name(), "敏感文件扫描",
            RiskReportType.MARKDOWN_REPORT.name(), "Markdown 安全报告"
    );

    private final ProjectInfoService projectInfoService;
    private final ScanResultService scanResultService;
    private final AgentRuleService agentRuleService;
    private final RiskReportService riskReportService;

    public TimelineServiceImpl(ProjectInfoService projectInfoService,
                               ScanResultService scanResultService,
                               AgentRuleService agentRuleService,
                               RiskReportService riskReportService) {
        this.projectInfoService = projectInfoService;
        this.scanResultService = scanResultService;
        this.agentRuleService = agentRuleService;
        this.riskReportService = riskReportService;
    }

    @Override
    public PageResult<TimelineEventVO> getProjectTimeline(Long projectId, long current, long size, String riskLevel) {
        validateProjectExists(projectId);
        List<TimelineEventVO> allEvents = collectAllEvents(projectId);

        if (riskLevel != null && !riskLevel.isBlank()) {
            String normalizedLevel = riskLevel.trim().toUpperCase();
            allEvents = allEvents.stream()
                    .filter(e -> normalizedLevel.equals(e.getRiskLevel()))
                    .toList();
        }

        allEvents.sort(Comparator.comparing(TimelineEventVO::getCreatedTime).reversed());

        int total = allEvents.size();
        int fromIndex = (int) ((current - 1) * size);
        int toIndex = Math.min(fromIndex + (int) size, total);

        List<TimelineEventVO> pageRecords = fromIndex < total
                ? allEvents.subList(fromIndex, toIndex)
                : List.of();

        long pages = (total + size - 1) / size;

        PageResult<TimelineEventVO> result = new PageResult<>();
        result.setRecords(pageRecords);
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        result.setPages(pages);
        return result;
    }

    @Override
    public ProjectSecurityOverviewVO getProjectOverview(Long projectId) {
        ProjectInfo projectInfo = loadProjectInfo(projectId);
        List<TimelineEventVO> allEvents = collectAllEvents(projectId);
        allEvents.sort(Comparator.comparing(TimelineEventVO::getCreatedTime).reversed());

        ProjectSecurityOverviewVO vo = new ProjectSecurityOverviewVO();
        vo.setProjectId(projectInfo.getId());
        vo.setProjectName(projectInfo.getProjectName());
        vo.setHasGit(projectInfo.getHasGit());
        vo.setHasAgentsMd(projectInfo.getHasAgentsMd());
        vo.setTotalEvents(allEvents.size());

        int criticalCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;
        RiskLevel highestLevel = RiskLevel.LOW;

        for (TimelineEventVO event : allEvents) {
            if (event.getRiskLevel() != null) {
                RiskLevel level = safeParseRiskLevel(event.getRiskLevel());
                if (level != null) {
                    highestLevel = highestLevel.max(level);
                    switch (level) {
                        case CRITICAL -> criticalCount++;
                        case HIGH -> highCount++;
                        case MEDIUM -> mediumCount++;
                        case LOW -> lowCount++;
                    }
                }
            }
        }

        vo.setCriticalCount(criticalCount);
        vo.setHighCount(highCount);
        vo.setMediumCount(mediumCount);
        vo.setLowCount(lowCount);
        vo.setHighestRiskLevel(highestLevel.name());

        vo.setLatestRiskLevel(allEvents.isEmpty() ? null : allEvents.get(0).getRiskLevel());

        ScanResult latestScan = scanResultService.lambdaQuery()
                .eq(ScanResult::getProjectId, projectId)
                .orderByDesc(ScanResult::getCreatedTime)
                .orderByDesc(ScanResult::getId)
                .last("limit 1")
                .one();
        vo.setLatestScanTime(latestScan != null ? latestScan.getCreatedTime() : null);

        RiskReport latestPreflight = riskReportService.lambdaQuery()
                .eq(RiskReport::getProjectId, projectId)
                .eq(RiskReport::getReportType, RiskReportType.PREFLIGHT_CHECK.name())
                .orderByDesc(RiskReport::getCreatedTime)
                .orderByDesc(RiskReport::getId)
                .last("limit 1")
                .one();
        vo.setLatestPreflightTime(latestPreflight != null ? latestPreflight.getCreatedTime() : null);

        RiskReport latestGitAudit = riskReportService.lambdaQuery()
                .eq(RiskReport::getProjectId, projectId)
                .eq(RiskReport::getReportType, RiskReportType.GIT_DIFF_AUDIT.name())
                .orderByDesc(RiskReport::getCreatedTime)
                .orderByDesc(RiskReport::getId)
                .last("limit 1")
                .one();
        vo.setLatestGitAuditTime(latestGitAudit != null ? latestGitAudit.getCreatedTime() : null);

        vo.setSuggestions(buildSuggestions(vo));

        return vo;
    }

    @Override
    public List<TimelineEventVO> getHighRiskEvents(Long projectId, int limit) {
        validateProjectExists(projectId);
        List<TimelineEventVO> allEvents = collectAllEvents(projectId);

        List<TimelineEventVO> highRisk = allEvents.stream()
                .filter(e -> e.getRiskLevel() != null)
                .filter(e -> {
                    RiskLevel level = safeParseRiskLevel(e.getRiskLevel());
                    return level == RiskLevel.HIGH || level == RiskLevel.CRITICAL;
                })
                .sorted(Comparator.comparing(TimelineEventVO::getCreatedTime).reversed())
                .limit(limit)
                .toList();

        return highRisk;
    }

    private List<TimelineEventVO> collectAllEvents(Long projectId) {
        List<TimelineEventVO> events = new ArrayList<>();
        collectScanResultEvents(projectId, events);
        collectAgentRuleEvents(projectId, events);
        collectRiskReportEvents(projectId, events);
        return events;
    }

    private void collectScanResultEvents(Long projectId, List<TimelineEventVO> events) {
        List<ScanResult> scanResults = scanResultService.lambdaQuery()
                .eq(ScanResult::getProjectId, projectId)
                .orderByDesc(ScanResult::getCreatedTime)
                .orderByDesc(ScanResult::getId)
                .list();

        for (ScanResult sr : scanResults) {
            TimelineEventVO event = new TimelineEventVO();
            event.setEventType("SCAN_RESULT");
            event.setEventName("项目扫描");
            event.setRiskLevel(sr.getRiskLevel());
            event.setSummary("项目扫描完成，识别到 " + safeInt(sr.getFileCount()) + " 个文件、"
                    + safeInt(sr.getDirectoryCount()) + " 个目录");
            event.setSourceId(sr.getId());
            event.setSourceType("PROJECT_SCAN");
            event.setCreatedTime(sr.getCreatedTime());
            events.add(event);
        }
    }

    private void collectAgentRuleEvents(Long projectId, List<TimelineEventVO> events) {
        List<AgentRule> rules = agentRuleService.lambdaQuery()
                .eq(AgentRule::getProjectId, projectId)
                .orderByDesc(AgentRule::getCreatedTime)
                .orderByDesc(AgentRule::getId)
                .list();

        for (AgentRule rule : rules) {
            TimelineEventVO event = new TimelineEventVO();
            event.setEventType("AGENT_RULE");
            event.setEventName("Agent 规则生成");
            event.setRiskLevel("LOW");
            event.setSummary("已生成 " + rule.getAgentType() + " 规则文件 " + rule.getFileName());
            event.setSourceId(rule.getId());
            event.setSourceType(rule.getAgentType());
            event.setCreatedTime(rule.getCreatedTime());
            events.add(event);
        }
    }

    private void collectRiskReportEvents(Long projectId, List<TimelineEventVO> events) {
        List<RiskReport> reports = riskReportService.lambdaQuery()
                .eq(RiskReport::getProjectId, projectId)
                .orderByDesc(RiskReport::getCreatedTime)
                .orderByDesc(RiskReport::getId)
                .list();

        for (RiskReport report : reports) {
            TimelineEventVO event = new TimelineEventVO();
            event.setEventType("RISK_REPORT");
            event.setSourceId(report.getId());
            event.setSourceType(report.getReportType());
            event.setCreatedTime(report.getCreatedTime());
            event.setRiskLevel(report.getRiskLevel());

            String reportType = report.getReportType();
            String eventName = REPORT_TYPE_EVENT_NAMES.getOrDefault(reportType, "风险报告");
            event.setEventName(eventName);
            event.setSummary(buildReportSummary(reportType, report.getRiskLevel()));

            events.add(event);
        }
    }

    private String buildReportSummary(String reportType, String riskLevel) {
        String levelText = riskLevel != null ? riskLevel : "UNKNOWN";
        if (RiskReportType.PERMISSION_ASSESS.name().equals(reportType)) {
            return "权限风险评估完成，风险等级为 " + levelText;
        } else if (RiskReportType.COMMAND_AUDIT.name().equals(reportType)) {
            return "命令审计完成，风险等级为 " + levelText;
        } else if (RiskReportType.PREFLIGHT_CHECK.name().equals(reportType)) {
            return "Agent 执行前预检完成，风险等级为 " + levelText;
        } else if (RiskReportType.GIT_DIFF_AUDIT.name().equals(reportType)) {
            return "Git Diff 审计完成，风险等级为 " + levelText;
        } else if (RiskReportType.SENSITIVE_FILE_SCAN.name().equals(reportType)) {
            return "敏感文件扫描完成，风险等级为 " + levelText;
        } else if (RiskReportType.MARKDOWN_REPORT.name().equals(reportType)) {
            return "Markdown 安全报告已生成";
        } else {
            return "风险报告完成，风险等级为 " + levelText;
        }
    }

    private List<String> buildSuggestions(ProjectSecurityOverviewVO vo) {
        List<String> suggestions = new ArrayList<>();

        if (vo.getTotalEvents() == null || vo.getTotalEvents() == 0) {
            suggestions.add("当前项目暂无安全事件，建议先执行项目扫描。");
            return suggestions;
        }

        if ("CRITICAL".equals(vo.getHighestRiskLevel())) {
            suggestions.add("当前项目存在严重风险事件，建议优先处理 CRITICAL 记录。");
        }
        if (vo.getHighCount() != null && vo.getHighCount() > 0) {
            suggestions.add("当前项目存在高风险历史记录，建议查看 HIGH 事件详情。");
        }
        if (!Boolean.TRUE.equals(vo.getHasGit())) {
            suggestions.add("当前项目未检测到 Git 仓库，建议初始化 Git 以提升回滚能力。");
        }
        if (!Boolean.TRUE.equals(vo.getHasAgentsMd())) {
            suggestions.add("当前项目尚未检测到 AGENTS.md，建议生成并写入 Agent 规则文件。");
        }
        if (vo.getLatestPreflightTime() == null) {
            suggestions.add("建议在下一次 AI Agent 执行前进行 Preflight Check。");
        }

        return suggestions;
    }

    private void validateProjectExists(Long projectId) {
        if (projectId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Project id cannot be null");
        }
        if (projectInfoService.getById(projectId) == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
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

    private RiskLevel safeParseRiskLevel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return RiskLevel.fromCode(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}

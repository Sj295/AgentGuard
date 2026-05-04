package com.agentguard.report;

import com.agentguard.common.JsonUtils;
import com.agentguard.entity.AgentRule;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.RiskReport;
import com.agentguard.entity.ScanResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class MarkdownSecurityReportGenerator {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String generate(MarkdownReportContext context,
                           boolean includeScanResult,
                           boolean includeAgentRules,
                           boolean includeRiskReports,
                           boolean includeGitAudit,
                           boolean includePreflight) {
        StringBuilder sb = new StringBuilder();

        appendHeader(sb, context);
        appendProjectOverview(sb, context);

        if (includeScanResult) {
            appendScanSummary(sb, context);
        }

        if (includeAgentRules) {
            appendAgentRuleStatus(sb, context);
        }

        if (includeRiskReports) {
            appendRiskReportSummary(sb, context);
        }

        appendPermissionAssessment(sb, context);
        appendCommandAudit(sb, context);

        if (includePreflight) {
            appendPreflightCheck(sb, context);
        }

        if (includeGitAudit) {
            appendGitDiffAudit(sb, context);
        }

        appendRecommendedActions(sb, context);

        return sb.toString();
    }

    private void appendHeader(StringBuilder sb, MarkdownReportContext context) {
        ProjectInfo project = context.getProjectInfo();
        String projectName = project.getProjectName() != null ? project.getProjectName() : "Unknown";
        sb.append("# ").append(projectName).append(" Security Report\n\n");
        sb.append("**Generated:** ").append(LocalDateTime.now().format(TIME_FORMAT)).append("\n\n");
    }

    private void appendProjectOverview(StringBuilder sb, MarkdownReportContext context) {
        ProjectInfo project = context.getProjectInfo();
        sb.append("## 1. Project Overview\n\n");
        sb.append("| Item | Value |\n");
        sb.append("|---|---|\n");
        sb.append("| Project Name | ").append(nullToDash(project.getProjectName())).append(" |\n");
        sb.append("| Project Path | ").append(nullToDash(project.getProjectPath())).append(" |\n");
        sb.append("| Project Type | ").append(nullToDash(project.getProjectType())).append(" |\n");
        sb.append("| Tech Stack | ").append(nullToDash(project.getTechStack())).append(" |\n");
        sb.append("| Git Repository | ").append(Boolean.TRUE.equals(project.getHasGit()) ? "Yes" : "No").append(" |\n");
        sb.append("| AGENTS.md Exists | ").append(Boolean.TRUE.equals(project.getHasAgentsMd()) ? "Yes" : "No").append(" |\n");
        sb.append("\n");
    }

    private void appendScanSummary(StringBuilder sb, MarkdownReportContext context) {
        sb.append("## 2. Latest Scan Summary\n\n");

        ScanResult scanResult = context.getLatestScanResult();
        if (scanResult == null) {
            sb.append("No scan result available. Please run project scan first.\n\n");
            return;
        }

        sb.append("| Item | Value |\n");
        sb.append("|---|---|\n");
        sb.append("| File Count | ").append(scanResult.getFileCount() != null ? scanResult.getFileCount() : "-").append(" |\n");
        sb.append("| Directory Count | ").append(scanResult.getDirectoryCount() != null ? scanResult.getDirectoryCount() : "-").append(" |\n");
        sb.append("| Risk Level | ").append(nullToDash(scanResult.getRiskLevel())).append(" |\n");
        sb.append("| Scan Time | ").append(formatTime(scanResult.getCreatedTime())).append(" |\n");
        sb.append("\n");

        List<String> detectedFiles = JsonUtils.parseStringList(scanResult.getDetectedFiles());
        if (!detectedFiles.isEmpty()) {
            sb.append("**Detected Files:**\n\n");
            for (String file : detectedFiles) {
                sb.append("- ").append(file).append("\n");
            }
            sb.append("\n");
        }

        List<String> sensitiveFiles = JsonUtils.parseStringList(scanResult.getSensitiveFiles());
        if (!sensitiveFiles.isEmpty()) {
            sb.append("**Sensitive Files:**\n\n");
            for (String file : sensitiveFiles) {
                sb.append("- ").append(file).append("\n");
            }
            sb.append("\n");
        }
    }

    private void appendAgentRuleStatus(StringBuilder sb, MarkdownReportContext context) {
        sb.append("## 3. Agent Rule Status\n\n");

        List<AgentRule> rules = context.getAgentRules();
        if (rules == null || rules.isEmpty()) {
            sb.append("No agent rules generated yet.\n\n");
            return;
        }

        sb.append("| Agent | File Name | Created Time | Updated Time |\n");
        sb.append("|---|---|---|---|\n");
        for (AgentRule rule : rules) {
            sb.append("| ").append(nullToDash(rule.getAgentType()))
              .append(" | ").append(nullToDash(rule.getFileName()))
              .append(" | ").append(formatTime(rule.getCreatedTime()))
              .append(" | ").append(formatTime(rule.getUpdatedTime()))
              .append(" |\n");
        }
        sb.append("\n");

        sb.append("**Agent File Mapping:**\n\n");
        sb.append("- CODEX → AGENTS.md\n");
        sb.append("- CLAUDE → CLAUDE.md\n");
        sb.append("- CURSOR → .cursor/rules/agentguard.mdc\n\n");
    }

    private void appendRiskReportSummary(StringBuilder sb, MarkdownReportContext context) {
        sb.append("## 4. Risk Report Summary\n\n");

        List<RiskReport> reports = context.getLatestRiskReports();
        if (reports == null || reports.isEmpty()) {
            sb.append("No risk reports available.\n\n");
            return;
        }

        sb.append("| Type | Risk Level | Score | Created Time |\n");
        sb.append("|---|---|---:|---|\n");
        for (RiskReport report : reports) {
            sb.append("| ").append(nullToDash(report.getReportType()))
              .append(" | ").append(nullToDash(report.getRiskLevel()))
              .append(" | ").append(report.getRiskScore() == null ? "-" : report.getRiskScore())
              .append(" | ").append(formatTime(report.getCreatedTime()))
              .append(" |\n");
        }
        sb.append("\n");
    }

    private void appendPermissionAssessment(StringBuilder sb, MarkdownReportContext context) {
        sb.append("## 5. Permission Assessment\n\n");

        RiskReport report = context.getLatestPermissionReport();
        if (report == null) {
            sb.append("No permission assessment report available.\n\n");
            return;
        }

        appendRiskLevelAndScore(sb, report);

        List<String> riskItems = JsonUtils.parseStringList(report.getRiskItems());
        if (!riskItems.isEmpty()) {
            sb.append("**Risk Items:**\n\n");
            for (String item : riskItems) {
                sb.append("- ").append(item).append("\n");
            }
            sb.append("\n");
        }

        List<String> suggestions = JsonUtils.parseStringList(report.getSuggestions());
        if (!suggestions.isEmpty()) {
            sb.append("**Suggestions:**\n\n");
            for (String item : suggestions) {
                sb.append("- ").append(item).append("\n");
            }
            sb.append("\n");
        }
    }

    private void appendCommandAudit(StringBuilder sb, MarkdownReportContext context) {
        sb.append("## 6. Command Audit\n\n");

        RiskReport report = context.getLatestCommandAuditReport();
        if (report == null) {
            sb.append("No command audit report available.\n\n");
            return;
        }

        appendRiskLevelAndScore(sb, report);

        List<String> riskItems = JsonUtils.parseStringList(report.getRiskItems());
        if (!riskItems.isEmpty()) {
            sb.append("**Risk Items:**\n\n");
            for (String item : riskItems) {
                sb.append("- ").append(item).append("\n");
            }
            sb.append("\n");
        }

        List<String> suggestions = JsonUtils.parseStringList(report.getSuggestions());
        if (!suggestions.isEmpty()) {
            sb.append("**Suggestions:**\n\n");
            for (String item : suggestions) {
                sb.append("- ").append(item).append("\n");
            }
            sb.append("\n");
        }
    }

    private void appendPreflightCheck(StringBuilder sb, MarkdownReportContext context) {
        sb.append("## 7. Preflight Check\n\n");

        RiskReport report = context.getLatestPreflightReport();
        if (report == null) {
            sb.append("No preflight check report available.\n\n");
            return;
        }

        appendRiskLevelAndScore(sb, report);

        List<String> riskItems = JsonUtils.parseStringList(report.getRiskItems());
        if (!riskItems.isEmpty()) {
            sb.append("**Risk Items:**\n\n");
            for (String item : riskItems) {
                sb.append("- ").append(item).append("\n");
            }
            sb.append("\n");
        }

        List<String> suggestions = JsonUtils.parseStringList(report.getSuggestions());
        if (!suggestions.isEmpty()) {
            sb.append("**Suggestions:**\n\n");
            for (String item : suggestions) {
                sb.append("- ").append(item).append("\n");
            }
            sb.append("\n");
        }
    }

    private void appendGitDiffAudit(StringBuilder sb, MarkdownReportContext context) {
        sb.append("## 8. Git Diff Audit\n\n");

        RiskReport report = context.getLatestGitAuditReport();
        if (report == null) {
            sb.append("No git diff audit report available.\n\n");
            return;
        }

        appendRiskLevelAndScore(sb, report);

        List<String> riskItems = JsonUtils.parseStringList(report.getRiskItems());
        if (!riskItems.isEmpty()) {
            sb.append("**Risk Items:**\n\n");
            for (String item : riskItems) {
                sb.append("- ").append(item).append("\n");
            }
            sb.append("\n");
        }

        List<String> suggestions = JsonUtils.parseStringList(report.getSuggestions());
        if (!suggestions.isEmpty()) {
            sb.append("**Suggestions:**\n\n");
            for (String item : suggestions) {
                sb.append("- ").append(item).append("\n");
            }
            sb.append("\n");
        }
    }

    private void appendRecommendedActions(StringBuilder sb, MarkdownReportContext context) {
        sb.append("## 9. Recommended Next Actions\n\n");

        List<String> actions = buildRecommendedActions(context);
        if (actions.isEmpty()) {
            sb.append("No specific actions recommended at this time.\n\n");
            return;
        }

        for (int i = 0; i < actions.size(); i++) {
            sb.append(i + 1).append(". ").append(actions.get(i)).append("\n");
        }
        sb.append("\n");
    }

    private List<String> buildRecommendedActions(MarkdownReportContext context) {
        List<String> actions = new ArrayList<>();
        ProjectInfo project = context.getProjectInfo();

        if (context.getLatestScanResult() == null) {
            actions.add("Execute project scan to establish baseline security assessment.");
        }

        if (context.getAgentRules() == null || context.getAgentRules().isEmpty()) {
            actions.add("Generate Agent rules for AI Coding Agent integration.");
        }

        if (!Boolean.TRUE.equals(project.getHasAgentsMd())) {
            actions.add("Write AGENTS.md to project directory to improve AI Agent context.");
        }

        if (!Boolean.TRUE.equals(project.getHasGit())) {
            actions.add("Initialize Git repository or create backup before allowing AI Agent modifications.");
        }

        RiskReport permissionReport = context.getLatestPermissionReport();
        if (permissionReport != null && isHighRisk(permissionReport.getRiskLevel())) {
            actions.add("Pause high-privilege Agent operations and address permission risks first.");
        }

        RiskReport commandReport = context.getLatestCommandAuditReport();
        if (commandReport != null && isHighRisk(commandReport.getRiskLevel())) {
            actions.add("Review and address command audit risks before proceeding.");
        }

        RiskReport preflightReport = context.getLatestPreflightReport();
        if (preflightReport != null && isHighRisk(preflightReport.getRiskLevel())) {
            actions.add("Address preflight check warnings before executing Agent tasks.");
        }

        RiskReport gitReport = context.getLatestGitAuditReport();
        if (gitReport != null) {
            actions.add("Review Git diff audit results before committing changes.");
        }

        ScanResult scanResult = context.getLatestScanResult();
        if (scanResult != null) {
            List<String> sensitiveFiles = JsonUtils.parseStringList(scanResult.getSensitiveFiles());
            if (!sensitiveFiles.isEmpty()) {
                actions.add("Ensure Agent rules explicitly forbid reading, printing, or modifying sensitive files.");
            }
        }

        return actions;
    }

    private boolean isHighRisk(String riskLevel) {
        return "HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel);
    }

    private void appendRiskLevelAndScore(StringBuilder sb, RiskReport report) {
        sb.append("**Risk Level:** ").append(nullToDash(report.getRiskLevel())).append("\n\n");
        if (report.getRiskScore() != null) {
            sb.append("**Risk Score:** ").append(report.getRiskScore()).append("\n\n");
        }
        if (report.getSummary() != null && !report.getSummary().isBlank()) {
            sb.append("**Summary:** ").append(report.getSummary()).append("\n\n");
        }
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(TIME_FORMAT) : "-";
    }
}

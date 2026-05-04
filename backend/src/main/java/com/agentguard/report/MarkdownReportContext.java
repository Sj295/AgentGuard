package com.agentguard.report;

import com.agentguard.entity.AgentRule;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.RiskReport;
import com.agentguard.entity.ScanResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MarkdownReportContext {

    private ProjectInfo projectInfo;
    private ScanResult latestScanResult;
    private List<AgentRule> agentRules;
    private List<RiskReport> latestRiskReports;
    private RiskReport latestPermissionReport;
    private RiskReport latestCommandAuditReport;
    private RiskReport latestGitAuditReport;
    private RiskReport latestPreflightReport;
}

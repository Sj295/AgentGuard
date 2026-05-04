package com.agentguard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MarkdownReportExportRequest {

    @NotNull(message = "Project id cannot be null")
    private Long projectId;

    private Boolean includeScanResult = true;

    private Boolean includeAgentRules = true;

    private Boolean includeRiskReports = true;

    private Boolean includeGitAudit = true;

    private Boolean includePreflight = true;

    private Boolean overwrite = false;
}

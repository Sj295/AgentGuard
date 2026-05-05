package com.agentguard.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiGitDiffAnalysisRequest {

    @NotNull(message = "Project id cannot be null")
    private Long projectId;

    @NotNull(message = "Git audit report id cannot be null")
    private Long gitAuditReportId;
}

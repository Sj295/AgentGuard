package com.agentguard.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiRiskExplainRequest {

    @NotNull(message = "Project id cannot be null")
    private Long projectId;

    @NotNull(message = "Report id cannot be null")
    private Long reportId;
}

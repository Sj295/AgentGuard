package com.agentguard.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiReportSummaryRequest {

    @NotNull(message = "Project id cannot be null")
    private Long projectId;

    @NotBlank(message = "Markdown cannot be blank")
    private String markdown;
}

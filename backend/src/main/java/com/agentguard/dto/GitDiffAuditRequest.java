package com.agentguard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GitDiffAuditRequest {

    @NotNull(message = "Project id cannot be null")
    private Long projectId;
}

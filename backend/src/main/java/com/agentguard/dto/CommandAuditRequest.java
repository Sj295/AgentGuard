package com.agentguard.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CommandAuditRequest {

    @NotNull(message = "Project id cannot be null")
    private Long projectId;

    @NotEmpty(message = "Commands cannot be empty")
    private List<String> commands;
}

package com.agentguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PreflightCheckRequest {

    @NotNull(message = "Project id cannot be null")
    private Long projectId;

    @NotBlank(message = "Agent type cannot be blank")
    private String agentType;

    @NotBlank(message = "Task type cannot be blank")
    private String taskType;

    @NotBlank(message = "Sandbox mode cannot be blank")
    private String sandboxMode;

    @NotBlank(message = "Approval policy cannot be blank")
    private String approvalPolicy;

    @NotNull(message = "Network access cannot be null")
    private Boolean networkAccess;

    @NotNull(message = "Allow delete cannot be null")
    private Boolean allowDelete;

    private List<String> plannedCommands;
}

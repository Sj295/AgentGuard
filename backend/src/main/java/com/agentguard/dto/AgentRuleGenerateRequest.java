package com.agentguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentRuleGenerateRequest {

    @NotNull(message = "Project id cannot be null")
    private Long projectId;

    @NotBlank(message = "Agent type cannot be blank")
    private String agentType;
}

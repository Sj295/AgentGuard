package com.agentguard.dto;

import lombok.Data;

@Data
public class AgentRuleWriteRequest {

    private Boolean overwrite = false;

    private Boolean backup = true;

    private String agentType;
}

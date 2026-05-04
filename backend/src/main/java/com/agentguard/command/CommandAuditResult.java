package com.agentguard.command;

import com.agentguard.common.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CommandAuditResult {

    private RiskLevel riskLevel;
    private Integer score;
    private List<String> riskItems;
    private List<String> suggestions;
    private List<String> safeAlternatives;
}

package com.agentguard.risk;

import com.agentguard.common.enums.RiskLevel;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PermissionAssessResult {

    private RiskLevel riskLevel;

    private Integer score;

    private List<String> riskItems;

    private List<String> suggestions;

    private Map<String, Object> recommendedConfig;
}

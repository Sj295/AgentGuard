package com.agentguard.preflight;

import com.agentguard.common.enums.RiskLevel;
import com.agentguard.vo.PreflightCheckItemVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PreflightCheckResult {

    private RiskLevel overallRiskLevel;
    private Integer score;
    private Boolean allowedToProceed;
    private List<PreflightCheckItemVO> checkItems;
    private List<String> riskItems;
    private List<String> suggestions;
    private List<String> recommendedActions;
}

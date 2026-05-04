package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PreflightCheckVO {

    private Long projectId;

    private String agentType;

    private String taskType;

    private String overallRiskLevel;

    private Integer score;

    private Boolean allowedToProceed;

    private List<PreflightCheckItemVO> checkItems;

    private List<String> riskItems;

    private List<String> suggestions;

    private List<String> recommendedActions;

    private LocalDateTime createdTime;
}

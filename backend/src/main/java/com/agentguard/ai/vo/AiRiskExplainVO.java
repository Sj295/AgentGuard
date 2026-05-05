package com.agentguard.ai.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiRiskExplainVO {

    private Long projectId;

    private Long reportId;

    private String riskSummary;

    private List<String> whyItMatters;

    private List<String> fixPlan;

    private List<String> safeNextSteps;

    private String confidenceNote;

    private boolean mocked;
}

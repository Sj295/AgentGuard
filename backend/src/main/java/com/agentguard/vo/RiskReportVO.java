package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RiskReportVO {

    private Long id;

    private Long projectId;

    private String reportType;

    private String riskLevel;

    private Integer riskScore;

    private String summary;

    private String riskItems;

    private String suggestions;

    private String payloadJson;

    private LocalDateTime createdTime;
}

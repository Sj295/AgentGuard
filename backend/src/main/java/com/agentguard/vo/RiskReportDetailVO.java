package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RiskReportDetailVO {

    private Long id;

    private Long projectId;

    private String reportType;

    private String riskLevel;

    private Integer riskScore;

    private String summary;

    private List<String> riskItems;

    private List<String> suggestions;

    private String payloadJson;

    private LocalDateTime createdTime;
}

package com.agentguard.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiAnalysisRecordVO {

    private Long id;

    private Long projectId;

    private String analysisType;

    private Long sourceReportId;

    private String provider;

    private String model;

    private Boolean mocked;

    private Boolean success;

    private Long latencyMs;

    private String inputSummary;

    private String outputContent;

    private String errorMessage;

    private LocalDateTime createdTime;
}

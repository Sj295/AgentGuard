package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TimelineEventVO {

    private String eventType;

    private String eventName;

    private String riskLevel;

    private String summary;

    private Long sourceId;

    private String sourceType;

    private LocalDateTime createdTime;
}

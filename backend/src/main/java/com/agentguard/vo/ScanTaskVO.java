package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScanTaskVO {

    private Long id;

    private Long projectId;

    private String taskType;

    private String status;

    private Integer progress;

    private String resultSummary;

    private String errorMessage;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}

package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScanResultVO {

    private Long id;

    private Long projectId;

    private Long taskId;

    private Integer fileCount;

    private Integer directoryCount;

    private String detectedFiles;

    private String detectedCommands;

    private String sensitiveFiles;

    private String riskLevel;

    private LocalDateTime createdTime;
}

package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScanResultDetailVO {

    private Long id;

    private Long projectId;

    private Long taskId;

    private Integer fileCount;

    private Integer directoryCount;

    private List<String> detectedFiles;

    private List<String> detectedCommands;

    private List<String> sensitiveFiles;

    private String riskLevel;

    private LocalDateTime createdTime;
}

package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommandAuditVO {

    private Long reportId;

    private Long projectId;

    private String riskLevel;

    private Integer score;

    private List<String> riskItems;

    private List<String> suggestions;

    private List<String> safeAlternatives;

    private LocalDateTime createdTime;
}

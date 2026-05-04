package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PermissionAssessVO {

    private Long reportId;

    private Long projectId;

    private String agentType;

    private String taskType;

    private String sandboxMode;

    private String approvalPolicy;

    private Boolean networkAccess;

    private Boolean allowDelete;

    private String riskLevel;

    private Integer score;

    private List<String> riskItems;

    private List<String> suggestions;

    private Map<String, Object> recommendedConfig;

    private LocalDateTime createdTime;
}

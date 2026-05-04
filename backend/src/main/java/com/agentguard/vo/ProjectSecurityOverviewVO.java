package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectSecurityOverviewVO {

    private Long projectId;

    private String projectName;

    private String latestRiskLevel;

    private String highestRiskLevel;

    private Integer totalEvents;

    private Integer criticalCount;

    private Integer highCount;

    private Integer mediumCount;

    private Integer lowCount;

    private LocalDateTime latestScanTime;

    private LocalDateTime latestPreflightTime;

    private LocalDateTime latestGitAuditTime;

    private Boolean hasGit;

    private Boolean hasAgentsMd;

    private List<String> suggestions;
}

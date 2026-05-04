package com.agentguard.vo;

import lombok.Data;

import java.util.List;

@Data
public class ProjectScanVO {

    private Long projectId;

    private Long taskId;

    private String projectName;

    private String projectPath;

    private String projectType;

    private List<String> techStack;

    private Long fileCount;

    private Long directoryCount;

    private Boolean hasGit;

    private Boolean hasAgentsMd;

    private List<String> detectedFiles;

    private List<String> sensitiveFiles;

    private String riskLevel;

    private List<String> suggestions;
}

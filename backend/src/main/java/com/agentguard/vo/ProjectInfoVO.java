package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectInfoVO {

    private Long id;

    private String projectName;

    private String projectPath;

    private String projectType;

    private String techStack;

    private Boolean hasGit;

    private Boolean hasAgentsMd;

    private String description;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}

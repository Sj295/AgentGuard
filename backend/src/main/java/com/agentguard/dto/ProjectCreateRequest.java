package com.agentguard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectCreateRequest {

    @NotBlank(message = "Project name cannot be blank")
    private String projectName;

    @NotBlank(message = "Project path cannot be blank")
    private String projectPath;

    private String projectType;

    private String techStack;

    private Boolean hasGit;

    private Boolean hasAgentsMd;

    private String description;
}

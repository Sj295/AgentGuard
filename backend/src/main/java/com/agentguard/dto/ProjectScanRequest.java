package com.agentguard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectScanRequest {

    @NotBlank(message = "Project name cannot be blank")
    private String projectName;

    @NotBlank(message = "Project path cannot be blank")
    private String projectPath;
}

package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GitDiffAuditVO {

    private Long reportId;

    private Long projectId;

    private Integer changedFileCount;

    private List<String> addedFiles;

    private List<String> modifiedFiles;

    private List<String> deletedFiles;

    private String riskLevel;

    private List<String> riskItems;

    private List<String> suggestions;

    private List<String> rollbackCommands;

    private LocalDateTime createdTime;
}

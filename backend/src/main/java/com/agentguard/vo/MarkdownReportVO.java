package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MarkdownReportVO {

    private Long projectId;

    private String projectName;

    private String fileName;

    private String targetPath;

    private Boolean written;

    private String markdown;

    private LocalDateTime createdTime;
}

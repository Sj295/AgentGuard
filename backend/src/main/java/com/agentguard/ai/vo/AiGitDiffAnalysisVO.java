package com.agentguard.ai.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiGitDiffAnalysisVO {

    private Long projectId;

    private Long gitAuditReportId;

    private String summary;

    private List<String> impactAreas;

    private List<String> testSuggestions;

    private List<String> rollbackSuggestions;

    private String confidenceNote;

    private Boolean cached;

    private boolean mocked;
}

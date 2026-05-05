package com.agentguard.ai.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiReportSummaryVO {

    private Long projectId;

    private String executiveSummary;

    private List<String> keyFindings;

    private List<String> priorityActions;

    private String confidenceNote;

    private Boolean cached;

    private boolean mocked;
}

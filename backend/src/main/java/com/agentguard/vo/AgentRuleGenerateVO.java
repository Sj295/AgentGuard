package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentRuleGenerateVO {

    private Long id;

    private Long projectId;

    private String agentType;

    private String fileName;

    private String suggestedPath;

    private String content;

    private LocalDateTime createdTime;
}

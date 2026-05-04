package com.agentguard.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AgentRuleVO {

    private Long id;

    private Long projectId;

    private String agentType;

    private String fileName;

    private String content;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}

package com.agentguard.vo;

import lombok.Data;

@Data
public class AgentRuleWriteVO {

    private Long ruleId;

    private Long projectId;

    private String agentType;

    private String fileName;

    private String targetPath;

    private String backupPath;

    private Boolean written;

    private Boolean overwritten;

    private String message;
}

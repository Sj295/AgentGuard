package com.agentguard.service;

import com.agentguard.dto.AgentRuleWriteRequest;
import com.agentguard.vo.AgentRuleWriteVO;

public interface AgentRuleFileService {

    AgentRuleWriteVO writeRuleById(Long ruleId, AgentRuleWriteRequest request);

    AgentRuleWriteVO writeLatestRule(Long projectId, AgentRuleWriteRequest request);
}

package com.agentguard.service;

import com.agentguard.dto.AgentRuleGenerateRequest;
import com.agentguard.vo.AgentRuleGenerateVO;
import com.agentguard.vo.AgentRuleVO;

import java.util.List;

public interface AgentRuleGenerateService {

    AgentRuleGenerateVO generateRule(AgentRuleGenerateRequest request);

    List<AgentRuleGenerateVO> listByProjectId(Long projectId);

    AgentRuleVO getRuleDetail(Long id);

    AgentRuleVO getLatestByProjectAndAgentType(Long projectId, String agentType);
}

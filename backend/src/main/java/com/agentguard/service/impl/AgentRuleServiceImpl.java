package com.agentguard.service.impl;

import com.agentguard.entity.AgentRule;
import com.agentguard.mapper.AgentRuleMapper;
import com.agentguard.service.AgentRuleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AgentRuleServiceImpl extends ServiceImpl<AgentRuleMapper, AgentRule> implements AgentRuleService {
}

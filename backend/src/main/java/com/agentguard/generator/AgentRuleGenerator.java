package com.agentguard.generator;

public interface AgentRuleGenerator {

    String getAgentType();

    String getFileName();

    String generate(AgentRuleGenerateContext context);
}

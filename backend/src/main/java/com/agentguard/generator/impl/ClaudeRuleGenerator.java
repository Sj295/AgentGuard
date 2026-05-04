package com.agentguard.generator.impl;

import com.agentguard.common.enums.AgentType;
import org.springframework.stereotype.Component;

@Component
public class ClaudeRuleGenerator extends AbstractAgentRuleGenerator {

    @Override
    public String getAgentType() {
        return AgentType.CLAUDE.getCode();
    }

    @Override
    public String getFileName() {
        return AgentType.CLAUDE.getFileName();
    }

    @Override
    protected void appendTitle(StringBuilder builder) {
        builder.append("# CLAUDE.md - AgentGuard Generated Rules\n\n");
    }

    @Override
    protected void appendAgentSpecificFocus(StringBuilder builder) {
        builder.append("## Claude Code Operating Focus\n\n")
                .append("- Use project context first, then apply coding conventions consistently.\n")
                .append("- Prefer safe tool usage patterns and confirm high-impact operations.\n")
                .append("- Explain assumptions when context is incomplete.\n\n");
    }
}

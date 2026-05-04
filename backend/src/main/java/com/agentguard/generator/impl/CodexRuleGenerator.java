package com.agentguard.generator.impl;

import com.agentguard.common.enums.AgentType;
import org.springframework.stereotype.Component;

@Component
public class CodexRuleGenerator extends AbstractAgentRuleGenerator {

    @Override
    public String getAgentType() {
        return AgentType.CODEX.getCode();
    }

    @Override
    public String getFileName() {
        return AgentType.CODEX.getFileName();
    }

    @Override
    protected void appendTitle(StringBuilder builder) {
        builder.append("# AGENTS.md - AgentGuard Generated Rules\n\n");
    }

    @Override
    protected void appendAgentSpecificFocus(StringBuilder builder) {
        builder.append("## Codex Operating Focus\n\n")
                .append("- Respect sandbox boundaries and explicit approval requirements.\n")
                .append("- Escalate for confirmation before any destructive command.\n")
                .append("- Keep each change auditable and tightly scoped.\n\n");
    }
}

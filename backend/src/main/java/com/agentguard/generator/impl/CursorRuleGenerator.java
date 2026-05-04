package com.agentguard.generator.impl;

import com.agentguard.common.enums.AgentType;
import org.springframework.stereotype.Component;

@Component
public class CursorRuleGenerator extends AbstractAgentRuleGenerator {

    @Override
    public String getAgentType() {
        return AgentType.CURSOR.getCode();
    }

    @Override
    public String getFileName() {
        return AgentType.CURSOR.getFileName();
    }

    @Override
    protected void appendTitle(StringBuilder builder) {
        builder.append("---\n")
                .append("description: AgentGuard generated safety and context rules\n")
                .append("globs:\n")
                .append("  - \"**/*\"\n")
                .append("alwaysApply: true\n")
                .append("---\n\n")
                .append("# AgentGuard Cursor Rules\n\n");
    }

    @Override
    protected void appendAgentSpecificFocus(StringBuilder builder) {
        builder.append("## Cursor Editor Focus\n\n")
                .append("- Apply these rules inside editor-driven coding workflows.\n")
                .append("- Avoid broad rewrite actions; keep edits minimal and traceable.\n")
                .append("- Require confirmation before any command that may remove or overwrite data.\n\n");
    }
}

package com.agentguard.generator;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AgentRuleGeneratorFactory {

    private final Map<String, AgentRuleGenerator> generatorMap;

    public AgentRuleGeneratorFactory(List<AgentRuleGenerator> generators) {
        this.generatorMap = new LinkedHashMap<>();
        for (AgentRuleGenerator generator : generators) {
            this.generatorMap.put(generator.getAgentType().toUpperCase(Locale.ROOT), generator);
        }
    }

    public AgentRuleGenerator getGenerator(String agentType) {
        if (agentType == null || agentType.isBlank()) {
            throw new IllegalArgumentException("Agent type cannot be blank");
        }
        AgentRuleGenerator generator = generatorMap.get(agentType.toUpperCase(Locale.ROOT));
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported agentType, expected one of: CODEX, CLAUDE, CURSOR");
        }
        return generator;
    }
}

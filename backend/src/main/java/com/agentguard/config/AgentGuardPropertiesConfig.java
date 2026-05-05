package com.agentguard.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RedisProperties.class, AiRateLimitProperties.class, AiCacheProperties.class})
public class AgentGuardPropertiesConfig {
}

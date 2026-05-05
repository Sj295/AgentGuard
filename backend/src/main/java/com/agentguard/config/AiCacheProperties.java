package com.agentguard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "agentguard.ai.cache")
public class AiCacheProperties {

    private boolean enabled = false;

    private int ttlSeconds = 3600;
}

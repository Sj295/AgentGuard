package com.agentguard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "agentguard.redis")
public class RedisProperties {

    private boolean enabled = false;
}

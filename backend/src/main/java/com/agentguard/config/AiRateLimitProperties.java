package com.agentguard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "agentguard.ai.rate-limit")
public class AiRateLimitProperties {

    private boolean enabled = false;

    private int perMinute = 10;

    private int perDay = 200;
}

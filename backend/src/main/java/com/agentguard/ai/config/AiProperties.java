package com.agentguard.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Data
@Component
@ConfigurationProperties(prefix = "agentguard.ai")
public class AiProperties {

    private boolean enabled = false;

    private String provider = "spring-ai-openai-compatible";

    private String baseUrl = "https://api.deepseek.com";

    private String apiKey = "";

    private String model = "deepseek-chat";

    private int timeoutSeconds = 30;

    private boolean mockOnEmptyKey = true;

    public boolean hasApiKey() {
        return StringUtils.hasText(apiKey);
    }
}

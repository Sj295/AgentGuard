package com.agentguard.ai.client;

import com.agentguard.ai.config.AiProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.http.HttpClient;
import java.time.Duration;

@Primary
@Component
public class SpringAiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiLlmClient.class);
    private static final double DEFAULT_TEMPERATURE = 0.2d;
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final String DEFAULT_COMPLETIONS_PATH = "/chat/completions";

    private final AiProperties aiProperties;

    public SpringAiLlmClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (!aiProperties.hasApiKey()) {
            throw new IllegalStateException("AI api key is empty");
        }
        try {
            String content = buildChatClient()
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
            if (!StringUtils.hasText(content)) {
                throw new IllegalStateException("AI response content is empty");
            }
            return content;
        } catch (RuntimeException exception) {
            log.warn("Spring AI request failed: {}", exception.getMessage());
            throw new IllegalStateException("Failed to call Spring AI provider", exception);
        }
    }

    protected ChatClient buildChatClient() {
        Duration timeout = Duration.ofSeconds(Math.max(1, aiProperties.getTimeoutSeconds()));
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(resolveBaseUrl())
                .apiKey(aiProperties.getApiKey().trim())
                .completionsPath(DEFAULT_COMPLETIONS_PATH)
                .restClientBuilder(org.springframework.web.client.RestClient.builder().requestFactory(requestFactory))
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(resolveModel())
                .temperature(DEFAULT_TEMPERATURE)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();

        return ChatClient.create(chatModel);
    }

    private String resolveBaseUrl() {
        if (!StringUtils.hasText(aiProperties.getBaseUrl())) {
            throw new IllegalStateException("AI baseUrl is empty");
        }
        return aiProperties.getBaseUrl().trim();
    }

    private String resolveModel() {
        if (!StringUtils.hasText(aiProperties.getModel())) {
            return DEFAULT_MODEL;
        }
        return aiProperties.getModel().trim();
    }
}

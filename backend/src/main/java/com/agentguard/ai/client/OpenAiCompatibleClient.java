package com.agentguard.ai.client;

import com.agentguard.ai.config.AiProperties;
import com.agentguard.ai.model.ChatCompletionRequest;
import com.agentguard.ai.model.ChatCompletionResponse;
import com.agentguard.ai.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Component
public class OpenAiCompatibleClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClient.class);

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public OpenAiCompatibleClient(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        Duration timeout = Duration.ofSeconds(Math.max(1, aiProperties.getTimeoutSeconds()));
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            throw new IllegalStateException("AI api key is empty");
        }
        if (!StringUtils.hasText(aiProperties.getBaseUrl())) {
            throw new IllegalStateException("AI baseUrl is empty");
        }
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(aiProperties.getModel());
        request.setTemperature(0.2);
        request.setMessages(List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userPrompt)
        ));

        try {
            ChatCompletionResponse response = restClient.post()
                    .uri(buildCompletionsUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
            return extractContent(response);
        } catch (RestClientException exception) {
            log.warn("OpenAI-compatible request failed: {}", exception.getMessage());
            throw new IllegalStateException("Failed to call AI provider", exception);
        }
    }

    private String buildCompletionsUrl() {
        String baseUrl = aiProperties.getBaseUrl().trim();
        if (baseUrl.endsWith("/")) {
            return baseUrl + "chat/completions";
        }
        return baseUrl + "/chat/completions";
    }

    private String extractContent(ChatCompletionResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new IllegalStateException("AI response is empty");
        }
        ChatCompletionResponse.Choice choice = response.getChoices().get(0);
        if (choice.getMessage() == null || !StringUtils.hasText(choice.getMessage().getContent())) {
            throw new IllegalStateException("AI response content is empty");
        }
        return choice.getMessage().getContent();
    }
}

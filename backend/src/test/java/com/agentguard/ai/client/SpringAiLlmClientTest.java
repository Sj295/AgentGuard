package com.agentguard.ai.client;

import com.agentguard.ai.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpringAiLlmClientTest {

    @Test
    void chat_shouldReturnContent() {
        AiProperties properties = new AiProperties();
        properties.setApiKey("test-key");

        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system("system").user("user").call().content()).thenReturn("{\"ok\":true}");

        SpringAiLlmClient client = new SpringAiLlmClient(properties) {
            @Override
            protected ChatClient buildChatClient() {
                return chatClient;
            }
        };
        String content = client.chat("system", "user");

        assertEquals("{\"ok\":true}", content);
    }

    @Test
    void chat_whenSpringAiFails_shouldThrowFriendlyException() {
        AiProperties properties = new AiProperties();
        properties.setApiKey("test-key");

        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system("system").user("user").call().content())
                .thenThrow(new RuntimeException("network timeout"));

        SpringAiLlmClient client = new SpringAiLlmClient(properties) {
            @Override
            protected ChatClient buildChatClient() {
                return chatClient;
            }
        };

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> client.chat("system", "user"));
        assertEquals("Failed to call Spring AI provider", exception.getMessage());
    }
}

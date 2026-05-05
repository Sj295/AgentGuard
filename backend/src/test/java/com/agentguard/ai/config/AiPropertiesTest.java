package com.agentguard.ai.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiPropertiesTest {

    @Test
    void defaults_shouldHaveCorrectValues() {
        AiProperties props = new AiProperties();

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getProvider()).isEqualTo("spring-ai-openai-compatible");
        assertThat(props.getBaseUrl()).isEqualTo("https://api.deepseek.com");
        assertThat(props.getApiKey()).isEmpty();
        assertThat(props.getModel()).isEqualTo("deepseek-chat");
        assertThat(props.getTimeoutSeconds()).isEqualTo(30);
        assertThat(props.isMockOnEmptyKey()).isTrue();
    }

    @Test
    void hasApiKey_shouldReturnFalseWhenEmpty() {
        AiProperties props = new AiProperties();
        props.setApiKey("");

        assertThat(props.hasApiKey()).isFalse();
    }

    @Test
    void hasApiKey_shouldReturnFalseWhenNull() {
        AiProperties props = new AiProperties();
        props.setApiKey(null);

        assertThat(props.hasApiKey()).isFalse();
    }

    @Test
    void hasApiKey_shouldReturnFalseWhenBlank() {
        AiProperties props = new AiProperties();
        props.setApiKey("   ");

        assertThat(props.hasApiKey()).isFalse();
    }

    @Test
    void hasApiKey_shouldReturnTrueWhenSet() {
        AiProperties props = new AiProperties();
        props.setApiKey("sk-test-key");

        assertThat(props.hasApiKey()).isTrue();
    }

    @Test
    void hasApiKey_shouldReturnTrueForWhitespaceWrappedKey() {
        AiProperties props = new AiProperties();
        props.setApiKey("  sk-test-key  ");

        assertThat(props.hasApiKey()).isTrue();
    }

    @Test
    void setters_shouldWorkCorrectly() {
        AiProperties props = new AiProperties();
        props.setEnabled(true);
        props.setProvider("custom-provider");
        props.setBaseUrl("https://custom.api.com");
        props.setApiKey("custom-key");
        props.setModel("custom-model");
        props.setTimeoutSeconds(60);
        props.setMockOnEmptyKey(false);

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getProvider()).isEqualTo("custom-provider");
        assertThat(props.getBaseUrl()).isEqualTo("https://custom.api.com");
        assertThat(props.getApiKey()).isEqualTo("custom-key");
        assertThat(props.getModel()).isEqualTo("custom-model");
        assertThat(props.getTimeoutSeconds()).isEqualTo(60);
        assertThat(props.isMockOnEmptyKey()).isFalse();
    }
}

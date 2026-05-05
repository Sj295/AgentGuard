package com.agentguard.ai.client;

public interface LlmClient {

    String chat(String systemPrompt, String userPrompt);
}

package com.agentguard.ai.model;

import lombok.Data;

import java.util.List;

@Data
public class ChatCompletionRequest {

    private String model;

    private List<ChatMessage> messages;

    private Double temperature;
}

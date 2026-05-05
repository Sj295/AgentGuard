package com.agentguard.ai.model;

import lombok.Data;

import java.util.List;

@Data
public class ChatCompletionResponse {

    private String id;

    private String model;

    private List<Choice> choices;

    @Data
    public static class Choice {
        private Integer index;
        private ChatMessage message;
        private String finishReason;
    }
}

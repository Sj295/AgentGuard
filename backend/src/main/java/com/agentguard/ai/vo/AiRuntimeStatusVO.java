package com.agentguard.ai.vo;

import lombok.Data;

@Data
public class AiRuntimeStatusVO {

    private Boolean enabled;

    private Boolean hasApiKey;

    private Boolean mockOnEmptyKey;

    private Boolean willCallRemoteModel;

    private String executionMode;

    private String provider;

    private String model;

    private String baseUrl;

    private String statusText;

    private String confidenceNote;
}

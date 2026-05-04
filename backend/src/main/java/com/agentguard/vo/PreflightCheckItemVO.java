package com.agentguard.vo;

import com.agentguard.common.enums.PreflightCheckStatus;
import lombok.Data;

@Data
public class PreflightCheckItemVO {

    private String name;

    private PreflightCheckStatus status;

    private String message;
}

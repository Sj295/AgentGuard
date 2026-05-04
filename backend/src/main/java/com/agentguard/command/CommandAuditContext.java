package com.agentguard.command;

import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.ScanResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CommandAuditContext {

    private Long projectId;
    private ProjectInfo projectInfo;
    private ScanResult latestScanResult;
    private List<String> commands;
    private boolean hasScanResult;
}

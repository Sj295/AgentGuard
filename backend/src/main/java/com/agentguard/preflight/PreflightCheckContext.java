package com.agentguard.preflight;

import com.agentguard.common.enums.AgentType;
import com.agentguard.common.enums.ApprovalPolicy;
import com.agentguard.common.enums.RiskLevel;
import com.agentguard.common.enums.SandboxMode;
import com.agentguard.common.enums.TaskType;
import com.agentguard.entity.AgentRule;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.ScanResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PreflightCheckContext {

    private Long projectId;
    private AgentType agentType;
    private TaskType taskType;
    private SandboxMode sandboxMode;
    private ApprovalPolicy approvalPolicy;
    private boolean networkAccess;
    private boolean allowDelete;
    private List<String> plannedCommands;

    private ProjectInfo projectInfo;
    private ScanResult latestScanResult;
    private boolean hasScanResult;
    private AgentRule latestAgentRule;
    private boolean hasAgentRule;
    private boolean ruleFileExistsOnDisk;
    private boolean isGitRepository;
    private int changedFileCount;
    private boolean hasGitChanges;
    private List<String> sensitiveFiles;
}

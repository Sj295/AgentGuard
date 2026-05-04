package com.agentguard.risk;

import com.agentguard.common.enums.AgentType;
import com.agentguard.common.enums.ApprovalPolicy;
import com.agentguard.common.enums.RiskLevel;
import com.agentguard.common.enums.SandboxMode;
import com.agentguard.common.enums.TaskType;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.ScanResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PermissionAssessContext {

    private Long projectId;

    private AgentType agentType;

    private TaskType taskType;

    private SandboxMode sandboxMode;

    private ApprovalPolicy approvalPolicy;

    private boolean networkAccess;

    private boolean allowDelete;

    private ProjectInfo projectInfo;

    private ScanResult latestScanResult;

    private RiskLevel latestScanRiskLevel;

    private List<String> latestSensitiveFiles;

    private boolean hasScanResult;
}

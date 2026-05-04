package com.agentguard.risk;

import com.agentguard.common.enums.AgentType;
import com.agentguard.common.enums.ApprovalPolicy;
import com.agentguard.common.enums.RiskLevel;
import com.agentguard.common.enums.SandboxMode;
import com.agentguard.common.enums.TaskType;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PermissionRiskAssessorTest {

    private PermissionRiskAssessor assessor;

    @BeforeEach
    void setUp() {
        assessor = new PermissionRiskAssessor();
    }

    @Test
    void assess_withNormalConfig_shouldReturnLowOrMedium() {
        ProjectInfo projectInfo = buildProjectInfo(true, true);
        PermissionAssessContext context = PermissionAssessContext.builder()
                .projectId(1L)
                .agentType(AgentType.CODEX)
                .taskType(TaskType.FRONTEND_REFACTOR)
                .sandboxMode(SandboxMode.WORKSPACE_WRITE)
                .approvalPolicy(ApprovalPolicy.ON_REQUEST)
                .networkAccess(false)
                .allowDelete(false)
                .projectInfo(projectInfo)
                .latestScanResult(buildScanResult())
                .latestScanRiskLevel(RiskLevel.LOW)
                .latestSensitiveFiles(List.of())
                .hasScanResult(true)
                .build();

        PermissionAssessResult result = assessor.assess(context);

        assertNotNull(result);
        assertNotNull(result.getRiskLevel());
        assertNotNull(result.getScore());
        assertTrue(result.getScore() >= 0 && result.getScore() <= 100);
        assertTrue(result.getRiskLevel() == RiskLevel.LOW || result.getRiskLevel() == RiskLevel.MEDIUM);
    }

    @Test
    void assess_withDangerFullAccessAndNeverApproval_shouldReturnCritical() {
        ProjectInfo projectInfo = buildProjectInfo(true, true);
        PermissionAssessContext context = PermissionAssessContext.builder()
                .projectId(1L)
                .agentType(AgentType.CODEX)
                .taskType(TaskType.LARGE_REFACTOR)
                .sandboxMode(SandboxMode.DANGER_FULL_ACCESS)
                .approvalPolicy(ApprovalPolicy.NEVER)
                .networkAccess(true)
                .allowDelete(true)
                .projectInfo(projectInfo)
                .latestScanResult(buildScanResult())
                .latestScanRiskLevel(RiskLevel.HIGH)
                .latestSensitiveFiles(List.of(".env", "id_rsa"))
                .hasScanResult(true)
                .build();

        PermissionAssessResult result = assessor.assess(context);

        assertEquals(RiskLevel.CRITICAL, result.getRiskLevel());
        assertTrue(result.getScore() >= 86);
        assertFalse(result.getRiskItems().isEmpty());
    }

    @Test
    void assess_withNoGitAndAllowDelete_shouldElevateRisk() {
        ProjectInfo projectInfo = buildProjectInfo(false, false);
        PermissionAssessContext context = PermissionAssessContext.builder()
                .projectId(1L)
                .agentType(AgentType.CURSOR)
                .taskType(TaskType.BUG_FIX)
                .sandboxMode(SandboxMode.WORKSPACE_WRITE)
                .approvalPolicy(ApprovalPolicy.ON_REQUEST)
                .networkAccess(false)
                .allowDelete(true)
                .projectInfo(projectInfo)
                .latestScanResult(null)
                .latestScanRiskLevel(null)
                .latestSensitiveFiles(List.of())
                .hasScanResult(false)
                .build();

        PermissionAssessResult result = assessor.assess(context);

        assertTrue(result.getRiskLevel() == RiskLevel.HIGH || result.getRiskLevel() == RiskLevel.CRITICAL);
        assertTrue(result.getRiskItems().stream().anyMatch(item -> item.contains("Git")));
    }

    @Test
    void assess_withFrontendCompatibleEnums_shouldReturnValidResult() {
        ProjectInfo projectInfo = buildProjectInfo(true, true);
        PermissionAssessContext context = PermissionAssessContext.builder()
                .projectId(1L)
                .agentType(AgentType.CLAUDE)
                .taskType(TaskType.NEW_FEATURE)
                .sandboxMode(SandboxMode.WORKSPACE_WRITE)
                .approvalPolicy(ApprovalPolicy.AUTO_APPROVE)
                .networkAccess(false)
                .allowDelete(false)
                .projectInfo(projectInfo)
                .latestScanResult(buildScanResult())
                .latestScanRiskLevel(RiskLevel.LOW)
                .latestSensitiveFiles(List.of())
                .hasScanResult(true)
                .build();

        PermissionAssessResult result = assessor.assess(context);

        assertNotNull(result);
        assertTrue(result.getScore() > 0);
        assertTrue(result.getSuggestions().stream().anyMatch(item -> item.contains("ON_REQUEST")));
    }

    private ProjectInfo buildProjectInfo(boolean hasGit, boolean hasAgentsMd) {
        ProjectInfo info = new ProjectInfo();
        info.setId(1L);
        info.setProjectName("TestProject");
        info.setProjectPath("/tmp/test");
        info.setHasGit(hasGit);
        info.setHasAgentsMd(hasAgentsMd);
        return info;
    }

    private ScanResult buildScanResult() {
        ScanResult sr = new ScanResult();
        sr.setId(1L);
        sr.setProjectId(1L);
        sr.setFileCount(100);
        sr.setDirectoryCount(20);
        sr.setRiskLevel("LOW");
        return sr;
    }
}

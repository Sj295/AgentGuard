package com.agentguard.preflight;

import com.agentguard.audit.GitDiffAuditor;
import com.agentguard.command.CommandAuditContext;
import com.agentguard.command.CommandAuditResult;
import com.agentguard.command.CommandRiskAuditor;
import com.agentguard.common.JsonUtils;
import com.agentguard.common.enums.AgentType;
import com.agentguard.common.enums.ApprovalPolicy;
import com.agentguard.common.enums.PreflightCheckStatus;
import com.agentguard.common.enums.RiskLevel;
import com.agentguard.common.enums.SandboxMode;
import com.agentguard.common.enums.TaskType;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.ScanResult;
import com.agentguard.risk.PermissionAssessContext;
import com.agentguard.risk.PermissionAssessResult;
import com.agentguard.risk.PermissionRiskAssessor;
import com.agentguard.vo.PreflightCheckItemVO;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class PreflightChecker {

    private static final Set<String> AGENT_RULE_FILE_NAMES = Set.of(
            "AGENTS.md",
            "CLAUDE.md",
            ".cursor/rules/agentguard.mdc"
    );

    private final PermissionRiskAssessor permissionRiskAssessor;
    private final CommandRiskAuditor commandRiskAuditor;
    private final GitDiffAuditor gitDiffAuditor;

    public PreflightChecker(PermissionRiskAssessor permissionRiskAssessor,
                            CommandRiskAuditor commandRiskAuditor,
                            GitDiffAuditor gitDiffAuditor) {
        this.permissionRiskAssessor = permissionRiskAssessor;
        this.commandRiskAuditor = commandRiskAuditor;
        this.gitDiffAuditor = gitDiffAuditor;
    }

    public PreflightCheckResult check(PreflightCheckContext context) {
        List<PreflightCheckItemVO> checkItems = new ArrayList<>();
        LinkedHashSet<String> riskItems = new LinkedHashSet<>();
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        LinkedHashSet<String> recommendedActions = new LinkedHashSet<>();
        int score = 0;

        score += checkProjectPath(context, checkItems);
        score += checkGitRepository(context, checkItems, riskItems, suggestions, recommendedActions);
        score += checkGitWorkingTree(context, checkItems, riskItems, suggestions);
        score += checkAgentRuleFile(context, checkItems, suggestions, recommendedActions);
        score += checkSensitiveFiles(context, checkItems, riskItems, suggestions);
        int permissionScore = checkPermissionConfig(context, checkItems, riskItems, suggestions);
        score += permissionScore;
        int commandScore = checkCommandRisk(context, checkItems, riskItems, suggestions);
        score += commandScore;
        score += checkTaskType(context, checkItems, suggestions);

        score = Math.max(0, Math.min(score, 100));
        RiskLevel overallLevel = RiskLevel.fromScore(score);
        overallLevel = applyOverrideRules(context, overallLevel, riskItems);
        score = adjustScoreForOverrides(context, score);

        boolean allowedToProceed = determineAllowed(context, overallLevel, checkItems);

        if (suggestions.isEmpty()) {
            suggestions.add("建议在执行任务前确认所有配置和文件状态。");
        }

        return PreflightCheckResult.builder()
                .overallRiskLevel(overallLevel)
                .score(score)
                .allowedToProceed(allowedToProceed)
                .checkItems(checkItems)
                .riskItems(new ArrayList<>(riskItems))
                .suggestions(new ArrayList<>(suggestions))
                .recommendedActions(new ArrayList<>(recommendedActions))
                .build();
    }

    private int checkProjectPath(PreflightCheckContext context, List<PreflightCheckItemVO> checkItems) {
        PreflightCheckItemVO item = new PreflightCheckItemVO();
        item.setName("项目存在性检查");

        ProjectInfo projectInfo = context.getProjectInfo();
        if (projectInfo == null || projectInfo.getProjectPath() == null || projectInfo.getProjectPath().isBlank()) {
            item.setStatus(PreflightCheckStatus.FAIL);
            item.setMessage("项目路径为空");
            checkItems.add(item);
            return 30;
        }

        Path projectPath = Paths.get(projectInfo.getProjectPath());
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            item.setStatus(PreflightCheckStatus.FAIL);
            item.setMessage("项目路径不存在或不是有效目录");
            checkItems.add(item);
            return 30;
        }

        item.setStatus(PreflightCheckStatus.PASS);
        item.setMessage("项目路径有效");
        checkItems.add(item);
        return 0;
    }

    private int checkGitRepository(PreflightCheckContext context,
                                   List<PreflightCheckItemVO> checkItems,
                                   LinkedHashSet<String> riskItems,
                                   LinkedHashSet<String> suggestions,
                                   LinkedHashSet<String> recommendedActions) {
        PreflightCheckItemVO item = new PreflightCheckItemVO();
        item.setName("Git 仓库检查");

        if (context.isGitRepository()) {
            item.setStatus(PreflightCheckStatus.PASS);
            item.setMessage("当前项目已检测到 Git 仓库");
            checkItems.add(item);
            return 0;
        }

        if (context.isAllowDelete()) {
            item.setStatus(PreflightCheckStatus.FAIL);
            item.setMessage("当前允许删除文件且项目没有 Git 保护，回滚风险较高");
            riskItems.add("当前允许删除文件且项目没有 Git 保护，回滚风险较高。");
            checkItems.add(item);
            return 25;
        }

        item.setStatus(PreflightCheckStatus.WARN);
        item.setMessage("未检测到 Git 仓库，建议先初始化");
        suggestions.add("建议先初始化 Git 仓库或创建备份后再让 AI Agent 修改代码。");
        recommendedActions.add("在项目目录执行 git init 并提交当前代码");
        checkItems.add(item);
        return 20;
    }

    private int checkGitWorkingTree(PreflightCheckContext context,
                                    List<PreflightCheckItemVO> checkItems,
                                    LinkedHashSet<String> riskItems,
                                    LinkedHashSet<String> suggestions) {
        PreflightCheckItemVO item = new PreflightCheckItemVO();
        item.setName("Git 工作区检查");

        if (!context.isGitRepository()) {
            item.setStatus(PreflightCheckStatus.WARN);
            item.setMessage("非 Git 仓库，跳过工作区检查");
            checkItems.add(item);
            return 0;
        }

        int changedCount = context.getChangedFileCount();
        if (changedCount == 0) {
            item.setStatus(PreflightCheckStatus.PASS);
            item.setMessage("当前工作区没有未提交变更");
            checkItems.add(item);
            return 0;
        }

        if (changedCount <= 20) {
            item.setStatus(PreflightCheckStatus.WARN);
            item.setMessage("当前工作区有 " + changedCount + " 个未提交变更，建议先提交或暂存");
            suggestions.add("建议在执行 Agent 任务前先提交或暂存当前变更。");
            checkItems.add(item);
            return 10;
        }

        item.setStatus(PreflightCheckStatus.FAIL);
        item.setMessage("当前工作区有 " + changedCount + " 个未提交变更，变更范围过大");
        riskItems.add("Git 工作区存在大量未提交变更（" + changedCount + " 个文件），Agent 执行可能覆盖或丢失这些变更。");
        suggestions.add("建议在执行 Agent 任务前先提交或暂存当前变更。");
        checkItems.add(item);
        return 30;
    }

    private int checkAgentRuleFile(PreflightCheckContext context,
                                   List<PreflightCheckItemVO> checkItems,
                                   LinkedHashSet<String> suggestions,
                                   LinkedHashSet<String> recommendedActions) {
        PreflightCheckItemVO item = new PreflightCheckItemVO();
        item.setName("Agent 规则文件检查");

        String expectedFileName = getExpectedFileName(context.getAgentType());

        if (context.isRuleFileExistsOnDisk()) {
            item.setStatus(PreflightCheckStatus.PASS);
            item.setMessage("已检测到规则文件 " + expectedFileName);
            checkItems.add(item);
            return 0;
        }

        if (context.isHasAgentRule()) {
            item.setStatus(PreflightCheckStatus.WARN);
            item.setMessage("规则已生成但未写入项目目录，建议先写入 " + expectedFileName);
            suggestions.add("建议先生成并写入 " + expectedFileName + "。");
            recommendedActions.add("执行 POST /api/agent-rules/project/" + context.getProjectId() + "/write 写入规则文件");
            checkItems.add(item);

            if (context.getTaskType() == TaskType.LARGE_REFACTOR) {
                item.setStatus(PreflightCheckStatus.FAIL);
                item.setMessage("大规模重构任务需要 Agent 规则文件，但 " + expectedFileName + " 未写入项目目录");
                return 15;
            }
            return 10;
        }

        item.setStatus(PreflightCheckStatus.WARN);
        item.setMessage("未检测到已生成的 " + expectedFileName + "，建议先生成规则文件");
        suggestions.add("建议先生成并写入 " + expectedFileName + "。");
        recommendedActions.add("执行 POST /api/agent-rules/generate 生成规则");
        recommendedActions.add("执行 POST /api/agent-rules/project/" + context.getProjectId() + "/write 写入规则文件");
        checkItems.add(item);

        if (context.getTaskType() == TaskType.LARGE_REFACTOR) {
            item.setStatus(PreflightCheckStatus.FAIL);
            item.setMessage("大规模重构任务需要 Agent 规则文件，但 " + expectedFileName + " 未生成");
            return 15;
        }
        return 10;
    }

    private int checkSensitiveFiles(PreflightCheckContext context,
                                    List<PreflightCheckItemVO> checkItems,
                                    LinkedHashSet<String> riskItems,
                                    LinkedHashSet<String> suggestions) {
        PreflightCheckItemVO item = new PreflightCheckItemVO();
        item.setName("敏感文件检查");

        if (context.getSensitiveFiles() == null || context.getSensitiveFiles().isEmpty()) {
            item.setStatus(PreflightCheckStatus.PASS);
            item.setMessage("未检测到敏感文件");
            checkItems.add(item);
            return 0;
        }

        item.setStatus(PreflightCheckStatus.WARN);
        item.setMessage("项目存在 " + context.getSensitiveFiles().size() + " 个敏感文件，Agent 执行时需避免读取或修改");
        riskItems.add("项目存在敏感文件，Agent 执行过程中需要避免读取、打印或修改。");
        suggestions.add("建议在 Agent 规则文件中明确禁止读取、打印或修改敏感文件。");
        checkItems.add(item);
        return 15;
    }

    private int checkPermissionConfig(PreflightCheckContext context,
                                      List<PreflightCheckItemVO> checkItems,
                                      LinkedHashSet<String> riskItems,
                                      LinkedHashSet<String> suggestions) {
        PreflightCheckItemVO item = new PreflightCheckItemVO();
        item.setName("权限配置检查");

        PermissionAssessContext assessContext = PermissionAssessContext.builder()
                .projectId(context.getProjectId())
                .agentType(context.getAgentType())
                .taskType(context.getTaskType())
                .sandboxMode(context.getSandboxMode())
                .approvalPolicy(context.getApprovalPolicy())
                .networkAccess(context.isNetworkAccess())
                .allowDelete(context.isAllowDelete())
                .projectInfo(context.getProjectInfo())
                .latestScanResult(context.getLatestScanResult())
                .latestScanRiskLevel(context.getLatestScanResult() != null
                        ? RiskLevel.fromCode(context.getLatestScanResult().getRiskLevel()) : null)
                .latestSensitiveFiles(context.getSensitiveFiles() != null ? context.getSensitiveFiles() : List.of())
                .hasScanResult(context.isHasScanResult())
                .build();

        PermissionAssessResult assessResult = permissionRiskAssessor.assess(assessContext);

        riskItems.addAll(assessResult.getRiskItems());
        suggestions.addAll(assessResult.getSuggestions());

        switch (assessResult.getRiskLevel()) {
            case LOW -> {
                item.setStatus(PreflightCheckStatus.PASS);
                item.setMessage("权限配置风险较低");
            }
            case MEDIUM -> {
                item.setStatus(PreflightCheckStatus.WARN);
                item.setMessage("权限配置存在一定风险，建议保持人工审批");
            }
            default -> {
                item.setStatus(PreflightCheckStatus.FAIL);
                item.setMessage("权限配置风险较高：" + assessResult.getRiskLevel());
            }
        }
        checkItems.add(item);
        return (int) (assessResult.getScore() * 0.4);
    }

    private int checkCommandRisk(PreflightCheckContext context,
                                 List<PreflightCheckItemVO> checkItems,
                                 LinkedHashSet<String> riskItems,
                                 LinkedHashSet<String> suggestions) {
        PreflightCheckItemVO item = new PreflightCheckItemVO();
        item.setName("命令风险检查");

        if (context.getPlannedCommands() == null || context.getPlannedCommands().isEmpty()) {
            item.setStatus(PreflightCheckStatus.PASS);
            item.setMessage("未提供待执行命令，仅进行配置级预检");
            checkItems.add(item);
            return 0;
        }

        CommandAuditContext auditContext = CommandAuditContext.builder()
                .projectId(context.getProjectId())
                .projectInfo(context.getProjectInfo())
                .latestScanResult(context.getLatestScanResult())
                .commands(context.getPlannedCommands())
                .hasScanResult(context.isHasScanResult())
                .build();

        CommandAuditResult auditResult = commandRiskAuditor.audit(auditContext);

        riskItems.addAll(auditResult.getRiskItems());
        suggestions.addAll(auditResult.getSuggestions());

        switch (auditResult.getRiskLevel()) {
            case LOW -> {
                item.setStatus(PreflightCheckStatus.PASS);
                item.setMessage("未检测到高危命令");
            }
            case MEDIUM -> {
                item.setStatus(PreflightCheckStatus.WARN);
                item.setMessage("检测到中等风险命令，建议审查后执行");
            }
            default -> {
                item.setStatus(PreflightCheckStatus.FAIL);
                item.setMessage("检测到高危命令，风险等级：" + auditResult.getRiskLevel());
            }
        }
        checkItems.add(item);
        return (int) (auditResult.getScore() * 0.4);
    }

    private int checkTaskType(PreflightCheckContext context,
                              List<PreflightCheckItemVO> checkItems,
                              LinkedHashSet<String> suggestions) {
        int score = 0;

        if (context.getTaskType() == TaskType.LARGE_REFACTOR) {
            score += 10;
            suggestions.add("大规模重构建议拆分为多个小任务执行。");
        }

        if (context.getTaskType() == TaskType.NEW_FEATURE) {
            score += 5;
            suggestions.add("新功能开发建议先明确影响范围和验收测试。");
        }

        if (context.getTaskType() == TaskType.TEST_WRITING) {
            suggestions.add("测试编写任务建议运行对应测试套件验证。");
        }

        if (context.getTaskType() == TaskType.GIT_OPERATION && context.getApprovalPolicy() != ApprovalPolicy.ALWAYS) {
            score += 15;
            suggestions.add("Git 操作建议设置审批策略为 ALWAYS 并人工确认。");
        }

        if (context.getTaskType() == TaskType.DEPENDENCY_INSTALL && !context.isNetworkAccess()) {
            PreflightCheckItemVO item = new PreflightCheckItemVO();
            item.setName("任务类型检查");
            item.setStatus(PreflightCheckStatus.WARN);
            item.setMessage("依赖安装任务需要联网，但当前不允许联网，任务可能失败");
            checkItems.add(item);
        }

        return score;
    }

    private RiskLevel applyOverrideRules(PreflightCheckContext context, RiskLevel current,
                                         LinkedHashSet<String> riskItems) {
        if (context.isGitRepository() && context.isAllowDelete()) {
            // already handled in git check
        }
        if (context.isGitRepository() && context.getChangedFileCount() > 20) {
            current = current.max(RiskLevel.HIGH);
        }
        if (!context.isGitRepository() && context.isAllowDelete()) {
            current = current.max(RiskLevel.CRITICAL);
        }
        if (context.getSandboxMode() == SandboxMode.DANGER_FULL_ACCESS
                && isNoHumanApproval(context.getApprovalPolicy())) {
            current = current.max(RiskLevel.CRITICAL);
            riskItems.add("当前配置使用高权限模式且关闭人工审批，存在严重安全风险。");
        }
        return current;
    }

    private int adjustScoreForOverrides(PreflightCheckContext context, int score) {
        if (!context.isGitRepository() && context.isAllowDelete()) {
            score = Math.max(score, 85);
        }
        if (context.getSandboxMode() == SandboxMode.DANGER_FULL_ACCESS
                && isNoHumanApproval(context.getApprovalPolicy())) {
            score = Math.max(score, 90);
        }
        return Math.min(score, 100);
    }

    private boolean determineAllowed(PreflightCheckContext context, RiskLevel overallLevel,
                                     List<PreflightCheckItemVO> checkItems) {
        boolean hasFail = checkItems.stream().anyMatch(i -> i.getStatus() == PreflightCheckStatus.FAIL);
        if (hasFail) {
            return false;
        }
        if (overallLevel == RiskLevel.HIGH || overallLevel == RiskLevel.CRITICAL) {
            return false;
        }
        if (context.getSandboxMode() == SandboxMode.DANGER_FULL_ACCESS
                && isNoHumanApproval(context.getApprovalPolicy())) {
            return false;
        }
        if (context.isAllowDelete() && !context.isGitRepository()) {
            return false;
        }
        return true;
    }

    private String getExpectedFileName(AgentType agentType) {
        return switch (agentType) {
            case CODEX -> "AGENTS.md";
            case CLAUDE -> "CLAUDE.md";
            case CURSOR -> ".cursor/rules/agentguard.mdc";
        };
    }

    private boolean isNoHumanApproval(ApprovalPolicy approvalPolicy) {
        return approvalPolicy == ApprovalPolicy.NEVER || approvalPolicy == ApprovalPolicy.AUTO_APPROVE;
    }
}

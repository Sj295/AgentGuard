package com.agentguard.risk;

import com.agentguard.common.enums.ApprovalPolicy;
import com.agentguard.common.enums.RiskLevel;
import com.agentguard.common.enums.SandboxMode;
import com.agentguard.common.enums.TaskType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
public class PermissionRiskAssessor {

    public PermissionAssessResult assess(PermissionAssessContext context) {
        int score = 0;
        LinkedHashSet<String> riskItems = new LinkedHashSet<>();
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();

        score += scoreBySandbox(context.getSandboxMode());
        score += scoreByApproval(context.getApprovalPolicy());
        score += scoreByTaskType(context.getTaskType());
        if (context.isNetworkAccess()) {
            score += 15;
            riskItems.add("当前允许联网，需注意依赖安装、外部脚本和数据泄露风险。");
        } else if (context.getTaskType() == TaskType.DEPENDENCY_INSTALL) {
            riskItems.add("当前不允许联网，依赖安装任务可能失败。");
        }
        if (context.isAllowDelete()) {
            score += 25;
            riskItems.add("当前允许删除文件，需要特别关注误删风险。");
        }

        switch (context.getSandboxMode()) {
            case READ_ONLY -> riskItems.add("当前处于只读沙箱模式，代码改动能力受限。");
            case WORKSPACE_WRITE -> riskItems.add("当前允许修改工作区文件，可能影响项目代码。");
            case DANGER_FULL_ACCESS -> {
                riskItems.add("当前使用高权限模式，Agent 可能访问或修改工作区外资源。");
                suggestions.add("建议优先使用 WORKSPACE_WRITE 而不是 DANGER_FULL_ACCESS。");
            }
        }
        if (context.getApprovalPolicy() == ApprovalPolicy.NEVER || context.getApprovalPolicy() == ApprovalPolicy.AUTO_APPROVE) {
            riskItems.add("当前关闭人工审批，危险操作可能不会被拦截。");
            suggestions.add("建议保留 ON_REQUEST 审批策略。");
        } else if (context.getApprovalPolicy() == ApprovalPolicy.ON_FAILURE) {
            riskItems.add("当前采用失败后审批，部分风险动作可能先执行后提示。");
            suggestions.add("建议保留 ON_REQUEST 审批策略。");
        }
        if (context.getTaskType() == TaskType.LARGE_REFACTOR) {
            riskItems.add("大规模重构可能影响多个模块，建议拆分成小任务执行。");
            suggestions.add("建议大型重构拆分为多个小任务。");
        }
        if (context.getTaskType() == TaskType.DEPENDENCY_INSTALL) {
            riskItems.add("依赖安装可能引入供应链风险，建议确认依赖来源。");
            suggestions.add("建议在执行依赖安装前确认包名和来源。");
        }
        if (context.getTaskType() == TaskType.GIT_OPERATION) {
            riskItems.add("Git 操作可能影响版本历史和回滚能力。");
        }

        if (!Boolean.TRUE.equals(context.getProjectInfo().getHasGit())) {
            score += 15;
            suggestions.add("当前项目未检测到 Git 仓库，建议先初始化 Git 或创建备份后再执行 Agent 修改。");
        }
        if (!Boolean.TRUE.equals(context.getProjectInfo().getHasAgentsMd())
                && "CODEX".equals(context.getAgentType().getCode())) {
            score += 5;
            suggestions.add("建议先生成 AGENTS.md，降低 Codex 对项目上下文理解不足的风险。");
        }

        if (context.isHasScanResult()) {
            if (context.getLatestScanRiskLevel() == RiskLevel.HIGH) {
                score += 15;
            } else if (context.getLatestScanRiskLevel() == RiskLevel.CRITICAL) {
                score += 25;
            }
            if (context.getLatestSensitiveFiles() != null && !context.getLatestSensitiveFiles().isEmpty()) {
                score += 10;
                riskItems.add("项目存在敏感文件，Agent 执行过程中需要避免读取、打印或修改。");
            }
        } else {
            suggestions.add("建议先执行项目扫描以获得更准确的风险评估。");
            suggestions.add("建议先运行项目扫描并生成对应 Agent 规则文件。");
        }

        if (context.getSandboxMode() == SandboxMode.DANGER_FULL_ACCESS
                && isNoHumanApproval(context.getApprovalPolicy())
                && context.isAllowDelete()) {
            score = Math.max(score, 95);
            riskItems.add("当前配置允许 Agent 在无审批情况下进行高权限和删除操作，存在严重误删或越权风险。");
        }
        if (context.getTaskType() == TaskType.GIT_OPERATION && isNoHumanApproval(context.getApprovalPolicy())) {
            riskItems.add("Git 操作缺少人工审批，可能导致历史记录或工作区被破坏。");
        }
        if (context.isAllowDelete() && !Boolean.TRUE.equals(context.getProjectInfo().getHasGit())) {
            riskItems.add("当前允许删除文件且项目没有 Git 保护，回滚风险较高。");
        }

        suggestions.add("建议在 Git 新分支中执行该任务。");
        suggestions.add("建议任务完成后检查 Git Diff。");
        if (context.isAllowDelete()) {
            suggestions.add("建议禁止删除文件，除非任务明确需要。");
        }

        score = Math.max(0, Math.min(score, 100));
        RiskLevel riskLevel = RiskLevel.fromScore(score);
        if (context.getSandboxMode() == SandboxMode.DANGER_FULL_ACCESS
                && isNoHumanApproval(context.getApprovalPolicy())
                && context.isAllowDelete()) {
            riskLevel = riskLevel.max(RiskLevel.CRITICAL);
        }
        if (context.getTaskType() == TaskType.GIT_OPERATION && isNoHumanApproval(context.getApprovalPolicy())) {
            riskLevel = riskLevel.max(RiskLevel.HIGH);
        }
        if (context.isAllowDelete() && !Boolean.TRUE.equals(context.getProjectInfo().getHasGit())) {
            riskLevel = riskLevel.max(RiskLevel.HIGH);
        }

        return PermissionAssessResult.builder()
                .riskLevel(riskLevel)
                .score(score)
                .riskItems(new ArrayList<>(riskItems))
                .suggestions(new ArrayList<>(suggestions))
                .recommendedConfig(buildRecommendedConfig(context.getTaskType()))
                .build();
    }

    private int scoreBySandbox(SandboxMode sandboxMode) {
        return switch (sandboxMode) {
            case READ_ONLY -> 5;
            case WORKSPACE_WRITE -> 25;
            case DANGER_FULL_ACCESS -> 45;
        };
    }

    private int scoreByApproval(ApprovalPolicy approvalPolicy) {
        return switch (approvalPolicy) {
            case ALWAYS -> 5;
            case ON_REQUEST -> 10;
            case ON_FAILURE -> 20;
            case AUTO_APPROVE -> 30;
            case NEVER -> 35;
        };
    }

    private int scoreByTaskType(TaskType taskType) {
        return switch (taskType) {
            case READ_ONLY_ANALYSIS -> 0;
            case BUG_FIX -> 10;
            case FRONTEND_REFACTOR -> 15;
            case NEW_FEATURE -> 18;
            case TEST_WRITING -> 8;
            case DOCUMENTATION -> 5;
            case DEPENDENCY_INSTALL -> 20;
            case LARGE_REFACTOR -> 30;
            case GIT_OPERATION -> 30;
        };
    }

    private Map<String, Object> buildRecommendedConfig(TaskType taskType) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("sandboxMode", SandboxMode.WORKSPACE_WRITE.name());
        config.put("approvalPolicy", ApprovalPolicy.ON_REQUEST.name());
        config.put("networkAccess", false);
        config.put("allowDelete", false);

        switch (taskType) {
            case READ_ONLY_ANALYSIS -> config.put("sandboxMode", SandboxMode.READ_ONLY.name());
            case DOCUMENTATION -> config.put("sandboxMode", SandboxMode.READ_ONLY.name());
            case DEPENDENCY_INSTALL -> config.put("networkAccess", true);
            case LARGE_REFACTOR -> config.put("approvalPolicy", ApprovalPolicy.ALWAYS.name());
            case GIT_OPERATION -> config.put("approvalPolicy", ApprovalPolicy.ALWAYS.name());
            default -> {
            }
        }
        return config;
    }

    private boolean isNoHumanApproval(ApprovalPolicy approvalPolicy) {
        return approvalPolicy == ApprovalPolicy.NEVER || approvalPolicy == ApprovalPolicy.AUTO_APPROVE;
    }
}

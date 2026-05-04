package com.agentguard.command;

import com.agentguard.common.JsonUtils;
import com.agentguard.common.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class CommandRiskAuditor {

    private static final Pattern RM_RF = Pattern.compile("\\brm\\s+(-[a-zA-Z]*r[a-zA-Z]*f|-[a-zA-Z]*f[a-zA-Z]*r)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RM_FR = Pattern.compile("\\brm\\s+(-[a-zA-Z]*f[a-zA-Z]*r|-[a-zA-Z]*r[a-zA-Z]*f)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEL_S = Pattern.compile("\\bdel\\s+/s\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RMDIR_S = Pattern.compile("\\brmdir\\s+/s\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMOVE_ITEM_RECURSE = Pattern.compile("\\bRemove-Item\\b.*-Recurse", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMOVE_ITEM_FORCE = Pattern.compile("\\bRemove-Item\\b.*-Force", Pattern.CASE_INSENSITIVE);

    private static final Pattern GIT_RESET_HARD = Pattern.compile("\\bgit\\s+reset\\s+--hard\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GIT_CLEAN_XFD = Pattern.compile("\\bgit\\s+clean\\s+-[a-zA-Z]*x[a-zA-Z]*f[a-zA-Z]*d\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GIT_CLEAN_FD = Pattern.compile("\\bgit\\s+clean\\s+-(?!.*x)[a-zA-Z]*f[a-zA-Z]*d\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GIT_CHECKOUT_DOT = Pattern.compile("\\bgit\\s+checkout\\s+\\.\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GIT_RESTORE_DOT = Pattern.compile("\\bgit\\s+restore\\s+\\.\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GIT_PUSH_FORCE = Pattern.compile("\\bgit\\s+push\\s+.*--force\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GIT_REBASE = Pattern.compile("\\bgit\\s+rebase\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern CURL_PIPE = Pattern.compile("\\bcurl\\b.*\\|\\s*(sh|bash)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WGET_PIPE = Pattern.compile("\\bwget\\b.*\\|\\s*(sh|bash)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern IWR_PIPE = Pattern.compile("\\biwr\\b.*\\|\\s*iex\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern IRM_PIPE = Pattern.compile("\\birm\\b.*\\|\\s*iex\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern SUDO = Pattern.compile("\\bsudo\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHMOD_777 = Pattern.compile("\\bchmod\\s+(-R\\s+)?777\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHOWN_R = Pattern.compile("\\bchown\\s+-R\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SET_EXECUTION_POLICY = Pattern.compile("\\bSet-ExecutionPolicy\\b.*Unrestricted", Pattern.CASE_INSENSITIVE);

    private static final Pattern DOCKER_SYSTEM_PRUNE = Pattern.compile("\\bdocker\\s+system\\s+prune\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOCKER_VOLUME_PRUNE = Pattern.compile("\\bdocker\\s+volume\\s+prune\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOCKER_IMAGE_PRUNE_A = Pattern.compile("\\bdocker\\s+image\\s+prune\\b.*-a\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOCKER_CONTAINER_PRUNE = Pattern.compile("\\bdocker\\s+container\\s+prune\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern NPM_INSTALL = Pattern.compile("\\bnpm\\s+install\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PNPM_ADD = Pattern.compile("\\bpnpm\\s+add\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern YARN_ADD = Pattern.compile("\\byarn\\s+add\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PIP_INSTALL = Pattern.compile("\\bpip\\s+install\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MVN_DEP_GET = Pattern.compile("\\bmvn\\b.*dependency:get\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern CAT_ENV = Pattern.compile("\\b(cat|type)\\s+\\.env\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ECHO_VAR = Pattern.compile("\\becho\\s+\\$[A-Z_]+\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ECHO_PERCENT = Pattern.compile("\\becho\\s+%[A-Z_]+%\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRINTENV = Pattern.compile("\\b(printenv|env)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GET_CHILD_ITEM_ENV = Pattern.compile("\\bGet-ChildItem\\s+Env\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern DROP_DATABASE = Pattern.compile("\\bdrop\\s+database\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DROP_TABLE = Pattern.compile("\\bdrop\\s+table\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRUNCATE_TABLE = Pattern.compile("\\btruncate\\s+table\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_FROM = Pattern.compile("\\bdelete\\s+from\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MYSQL_E = Pattern.compile("\\bmysql\\b.*-e\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PSQL_C = Pattern.compile("\\bpsql\\b.*-c\\b", Pattern.CASE_INSENSITIVE);

    public CommandAuditResult audit(CommandAuditContext context) {
        Set<String> riskItemSet = new LinkedHashSet<>();
        Set<String> suggestionSet = new LinkedHashSet<>();
        Set<String> alternativeSet = new LinkedHashSet<>();
        RiskLevel maxRiskLevel = RiskLevel.LOW;
        int totalScore = 0;

        for (String command : context.getCommands()) {
            if (command == null || command.isBlank()) {
                continue;
            }
            String trimmed = command.trim();

            DetectionResult dr = detectSingle(trimmed);
            totalScore += dr.score;
            maxRiskLevel = maxRiskLevel.max(dr.riskLevel);
            riskItemSet.addAll(dr.riskItems);
            suggestionSet.addAll(dr.suggestions);
            alternativeSet.addAll(dr.alternatives);
        }

        maxRiskLevel = applyContextRules(context, riskItemSet, suggestionSet, alternativeSet, maxRiskLevel);

        totalScore = Math.min(totalScore, 100);
        RiskLevel finalLevel = maxRiskLevel.max(RiskLevel.fromScore(totalScore));
        totalScore = Math.max(totalScore, minScoreForLevel(finalLevel));

        if (suggestionSet.isEmpty()) {
            suggestionSet.add("建议在执行命令前确认操作影响范围。");
        }

        return CommandAuditResult.builder()
                .riskLevel(finalLevel)
                .score(totalScore)
                .riskItems(new ArrayList<>(riskItemSet))
                .suggestions(new ArrayList<>(suggestionSet))
                .safeAlternatives(new ArrayList<>(alternativeSet))
                .build();
    }

    private DetectionResult detectSingle(String command) {
        int score = 0;
        RiskLevel level = RiskLevel.LOW;
        List<String> risks = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();
        List<String> alternatives = new ArrayList<>();

        if (matchesAny(command, RM_RF, RM_FR, DEL_S, RMDIR_S, REMOVE_ITEM_RECURSE, REMOVE_ITEM_FORCE)) {
            score += 30;
            level = level.max(RiskLevel.HIGH);
            risks.add("检测到递归或强制删除命令，可能造成文件误删或项目损坏。");
            alternatives.add("删除目录前先执行 ls 或 dir 确认目标路径。");
            alternatives.add("建议使用 Git 管理项目，删除前确保有备份。");
        }

        if (matchesAny(command, GIT_RESET_HARD, GIT_CLEAN_XFD, GIT_PUSH_FORCE)) {
            score += 25;
            level = level.max(RiskLevel.HIGH);
            risks.add("检测到破坏性 Git 操作，可能丢失未提交修改或改写远程历史。");
            suggestions.add("建议执行前先检查 Git 工作区状态。");
            alternatives.add("如需撤销单个文件修改，优先使用 git restore <file>。");
            alternatives.add("建议先创建 Git 分支或备份。");
        }
        if (matchesAny(command, GIT_CLEAN_FD)) {
            score += 20;
            level = level.max(RiskLevel.MEDIUM);
            risks.add("检测到 git clean，可能删除未跟踪文件。");
            suggestions.add("建议先执行 git clean -n 预览将删除的文件。");
        }
        if (matchesAny(command, GIT_CHECKOUT_DOT, GIT_RESTORE_DOT)) {
            score += 15;
            level = level.max(RiskLevel.MEDIUM);
            risks.add("检测到 git checkout/restore .，将丢弃所有未暂存修改。");
            alternatives.add("如需保留部分修改，优先使用 git stash。");
        }
        if (matchesAny(command, GIT_REBASE)) {
            score += 15;
            level = level.max(RiskLevel.MEDIUM);
            risks.add("检测到 git rebase，可能改写提交历史。");
            suggestions.add("建议在 rebase 前创建备份分支。");
        }

        if (matchesAny(command, CURL_PIPE, WGET_PIPE, IWR_PIPE, IRM_PIPE)) {
            score += 45;
            level = level.max(RiskLevel.CRITICAL);
            risks.add("检测到远程脚本直接执行，存在严重供应链安全风险。");
            suggestions.add("建议避免直接执行远程脚本，先下载并人工审查脚本内容。");
            alternatives.add("将远程脚本下载到本地审查后再执行。");
        }

        if (matchesAny(command, SUDO)) {
            score += 20;
            level = level.max(RiskLevel.MEDIUM);
            risks.add("检测到 sudo 权限提升命令，可能扩大系统风险。");
        }
        if (matchesAny(command, CHMOD_777)) {
            score += 20;
            level = level.max(RiskLevel.MEDIUM);
            risks.add("检测到 chmod 777 过度授权，存在安全隐患。");
            alternatives.add("建议使用最小权限原则，如 chmod 755。");
        }
        if (matchesAny(command, CHOWN_R)) {
            score += 15;
            level = level.max(RiskLevel.MEDIUM);
            risks.add("检测到 chown -R 递归修改文件所有者，可能影响系统文件权限。");
        }
        if (matchesAny(command, SET_EXECUTION_POLICY)) {
            score += 20;
            level = level.max(RiskLevel.MEDIUM);
            risks.add("检测到 Set-ExecutionPolicy Unrestricted，可能降低系统安全策略。");
        }

        if (matchesAny(command, DOCKER_SYSTEM_PRUNE, DOCKER_VOLUME_PRUNE, DOCKER_IMAGE_PRUNE_A, DOCKER_CONTAINER_PRUNE)) {
            score += 20;
            level = level.max(RiskLevel.MEDIUM);
            risks.add("检测到 Docker 清理命令，可能删除镜像、容器、卷或缓存。");
            suggestions.add("建议先执行 docker system df 查看磁盘占用。");
            alternatives.add("建议指定 --filter 精确清理，避免误删。");
        }

        if (matchesAny(command, NPM_INSTALL, PNPM_ADD, YARN_ADD, PIP_INSTALL, MVN_DEP_GET)) {
            score += 15;
            level = level.max(RiskLevel.MEDIUM);
            risks.add("检测到依赖安装命令，需确认依赖来源和版本，避免供应链风险。");
            suggestions.add("建议固定依赖版本。");
            suggestions.add("建议检查 lock 文件变化。");
            suggestions.add("建议确认包来源可信。");
        }

        if (matchesAny(command, CAT_ENV, ECHO_VAR, ECHO_PERCENT, PRINTENV, GET_CHILD_ITEM_ENV)) {
            score += 30;
            level = level.max(RiskLevel.HIGH);
            risks.add("检测到可能读取或打印环境变量/密钥的命令，存在敏感信息泄露风险。");
            alternatives.add("建议在 CI/CD 环境中使用密钥管理服务，避免直接打印。");
        }

        if (matchesAny(command, DROP_DATABASE, DROP_TABLE, TRUNCATE_TABLE, DELETE_FROM, MYSQL_E, PSQL_C)) {
            score += 30;
            level = level.max(RiskLevel.HIGH);
            risks.add("检测到数据库破坏性操作，可能造成数据丢失。");
            suggestions.add("建议在执行前备份数据库。");
            alternatives.add("建议先在测试环境验证 SQL 语句。");
        }

        return new DetectionResult(level, score, risks, suggestions, alternatives);
    }

    private RiskLevel applyContextRules(CommandAuditContext context,
                                         Set<String> riskItems,
                                         Set<String> suggestions,
                                         Set<String> alternatives,
                                         RiskLevel currentLevel) {
        boolean hasDelete = context.getCommands().stream().anyMatch(cmd ->
                cmd != null && matchesAny(cmd.trim(), RM_RF, RM_FR, DEL_S, RMDIR_S, REMOVE_ITEM_RECURSE, REMOVE_ITEM_FORCE));

        if (hasDelete && context.getProjectInfo() != null && !Boolean.TRUE.equals(context.getProjectInfo().getHasGit())) {
            currentLevel = currentLevel.max(RiskLevel.CRITICAL);
            riskItems.add("当前项目没有 Git 保护，删除操作回滚风险极高。");
            alternatives.add("建议先初始化 Git 仓库并提交当前代码。");
        }

        boolean hasEnvRead = context.getCommands().stream().anyMatch(cmd ->
                cmd != null && matchesAny(cmd.trim(), CAT_ENV, ECHO_VAR, ECHO_PERCENT, PRINTENV, GET_CHILD_ITEM_ENV));

        if (hasEnvRead && context.getLatestScanResult() != null) {
            List<String> sensitiveFiles = JsonUtils.parseStringList(context.getLatestScanResult().getSensitiveFiles());
            if (!sensitiveFiles.isEmpty()) {
                currentLevel = currentLevel.max(RiskLevel.HIGH);
                riskItems.add("项目存在敏感文件，当前命令可能造成敏感信息泄露。");
            }
        }

        String techStack = context.getProjectInfo() != null ? context.getProjectInfo().getTechStack() : "";
        boolean hasNodeDeps = context.getCommands().stream().anyMatch(cmd ->
                cmd != null && matchesAny(cmd.trim(), NPM_INSTALL, PNPM_ADD, YARN_ADD));
        if (hasNodeDeps && techStack != null && techStack.toUpperCase(Locale.ROOT).contains("NODE")) {
            suggestions.add("建议检查 package.json 与 lock 文件是否同步变化。");
        }

        boolean hasJavaDeps = context.getCommands().stream().anyMatch(cmd ->
                cmd != null && (cmd.contains("mvn") || cmd.contains("gradle")));
        if (hasJavaDeps && techStack != null) {
            String upper = techStack.toUpperCase(Locale.ROOT);
            if (upper.contains("JAVA") || upper.contains("MAVEN") || upper.contains("GRADLE")) {
                suggestions.add("建议执行依赖树检查并关注版本冲突。");
            }
        }

        if (!context.isHasScanResult()) {
            suggestions.add("建议先执行项目扫描以获得更准确的命令风险评估。");
        }

        return currentLevel;
    }

    private int minScoreForLevel(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> 0;
            case MEDIUM -> 31;
            case HIGH -> 61;
            case CRITICAL -> 86;
        };
    }

    private boolean matchesAny(String command, Pattern... patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(command).find()) {
                return true;
            }
        }
        return false;
    }

    private record DetectionResult(RiskLevel riskLevel, int score, List<String> riskItems,
                                   List<String> suggestions, List<String> alternatives) {
    }
}

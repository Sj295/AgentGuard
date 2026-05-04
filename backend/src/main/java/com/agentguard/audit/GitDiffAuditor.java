package com.agentguard.audit;

import com.agentguard.common.enums.RiskLevel;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class GitDiffAuditor {

    private static final Set<String> SECURITY_KEYWORDS = Set.of(
            "auth", "authentication", "security", "login", "jwt", "token", "permission", "role"
    );
    private static final Set<String> PAYMENT_KEYWORDS = Set.of("payment", "pay", "order", "trade");
    private static final Set<String> DEPLOY_KEYWORDS = Set.of("dockerfile", "docker-compose.yml", "nginx.conf", "k8s", "deployment", "deploy");
    private static final Set<String> DEPENDENCY_FILES = Set.of(
            "pom.xml", "build.gradle", "settings.gradle", "package.json", "package-lock.json",
            "pnpm-lock.yaml", "yarn.lock", "requirements.txt", "pyproject.toml"
    );
    private static final Set<String> CRITICAL_CONFIG_FILES = Set.of(
            "application.yml", "application.yaml", "application-dev.yml", "application-prod.yml",
            "bootstrap.yml", ".env", "docker-compose.yml", "dockerfile"
    );

    private final GitCommandExecutor gitCommandExecutor;

    public GitDiffAuditor(GitCommandExecutor gitCommandExecutor) {
        this.gitCommandExecutor = gitCommandExecutor;
    }

    public GitDiffAuditResult audit(Path projectPath, List<String> techStack) {
        GitCommandExecutor.CommandResult statusResult = gitCommandExecutor.execute(projectPath, List.of("git", "status", "--porcelain"));
        ParsedGitStatus parsed = parsePorcelain(statusResult.getStdout());

        LinkedHashSet<String> addedFiles = parsed.addedFiles;
        LinkedHashSet<String> modifiedFiles = parsed.modifiedFiles;
        LinkedHashSet<String> deletedFiles = parsed.deletedFiles;

        LinkedHashSet<String> mergedChanged = new LinkedHashSet<>();
        mergedChanged.addAll(addedFiles);
        mergedChanged.addAll(modifiedFiles);
        mergedChanged.addAll(deletedFiles);
        int changedCount = mergedChanged.size();

        if (changedCount == 0) {
            return GitDiffAuditResult.builder()
                    .changedFileCount(0)
                    .addedFiles(List.of())
                    .modifiedFiles(List.of())
                    .deletedFiles(List.of())
                    .riskLevel(RiskLevel.LOW)
                    .riskItems(List.of())
                    .suggestions(List.of("当前工作区没有检测到未提交变更。"))
                    .rollbackCommands(List.of())
                    .build();
        }

        RiskLevel riskLevel = RiskLevel.LOW;
        LinkedHashSet<String> riskItems = new LinkedHashSet<>();
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();

        boolean touchedSecurity = false;
        boolean touchedSql = false;
        boolean modifiedPom = false;
        boolean modifiedPackageJson = false;

        for (String file : modifiedFiles) {
            String lower = file.toLowerCase(Locale.ROOT);
            String fileName = fileName(lower);
            if (containsAny(lower, SECURITY_KEYWORDS)) {
                touchedSecurity = true;
                riskLevel = riskLevel.max(RiskLevel.HIGH);
                riskItems.add("修改了认证或安全相关代码：" + file);
            }
            if (containsAny(lower, PAYMENT_KEYWORDS)) {
                riskLevel = riskLevel.max(RiskLevel.HIGH);
                riskItems.add("修改了支付或订单相关代码：" + file);
            }
            if (DEPENDENCY_FILES.contains(fileName)) {
                riskLevel = riskLevel.max(RiskLevel.MEDIUM);
                riskItems.add("修改了依赖配置文件：" + file);
                if ("pom.xml".equals(fileName)) {
                    modifiedPom = true;
                }
                if ("package.json".equals(fileName)) {
                    modifiedPackageJson = true;
                }
            }
            if (isDatabaseFile(lower)) {
                touchedSql = true;
                riskLevel = riskLevel.max(RiskLevel.MEDIUM);
                riskItems.add("修改了数据库相关文件：" + file);
            }
            if (isDeployFile(lower)) {
                riskLevel = riskLevel.max(RiskLevel.MEDIUM);
                riskItems.add("修改了部署相关文件：" + file);
            }
        }

        for (String file : deletedFiles) {
            String lower = file.toLowerCase(Locale.ROOT);
            riskLevel = riskLevel.max(RiskLevel.MEDIUM);
            riskItems.add("检测到文件删除操作：" + file);
            if (CRITICAL_CONFIG_FILES.contains(fileName(lower))) {
                riskLevel = riskLevel.max(RiskLevel.HIGH);
                riskItems.add("删除了关键配置文件：" + file);
            }
        }

        for (String renameEntry : parsed.renamedEntries) {
            riskItems.add("检测到文件重命名操作：" + renameEntry);
        }

        if (changedCount > 50) {
            riskLevel = riskLevel.max(RiskLevel.CRITICAL);
            riskItems.add("本次变更范围过大，存在难以审查和回滚的风险。");
        } else if (changedCount > 20) {
            riskLevel = riskLevel.max(RiskLevel.HIGH);
            riskItems.add("本次变更文件数量较多，建议拆分为多个小提交。");
        }

        addSuggestions(suggestions, techStack, touchedSecurity, touchedSql, modifiedPom, modifiedPackageJson, !deletedFiles.isEmpty());
        List<String> rollbackCommands = buildRollbackCommands(addedFiles, modifiedFiles, deletedFiles);

        return GitDiffAuditResult.builder()
                .changedFileCount(changedCount)
                .addedFiles(new ArrayList<>(addedFiles))
                .modifiedFiles(new ArrayList<>(modifiedFiles))
                .deletedFiles(new ArrayList<>(deletedFiles))
                .riskLevel(riskLevel)
                .riskItems(new ArrayList<>(riskItems))
                .suggestions(new ArrayList<>(suggestions))
                .rollbackCommands(rollbackCommands)
                .build();
    }

    private ParsedGitStatus parsePorcelain(String stdout) {
        LinkedHashSet<String> addedFiles = new LinkedHashSet<>();
        LinkedHashSet<String> modifiedFiles = new LinkedHashSet<>();
        LinkedHashSet<String> deletedFiles = new LinkedHashSet<>();
        LinkedHashSet<String> renamedEntries = new LinkedHashSet<>();
        if (stdout == null || stdout.isBlank()) {
            return new ParsedGitStatus(addedFiles, modifiedFiles, deletedFiles, renamedEntries);
        }

        String[] lines = stdout.split("\\R");
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (line.startsWith("?? ")) {
                addedFiles.add(normalizePath(line.substring(3).trim()));
                continue;
            }
            if (line.length() < 3) {
                continue;
            }

            char x = line.charAt(0);
            char y = line.charAt(1);
            String rawPath = line.substring(3).trim();
            if (rawPath.isBlank()) {
                continue;
            }

            if (isRenameStatus(x, y) && rawPath.contains(" -> ")) {
                String oldPath = normalizePath(rawPath.substring(0, rawPath.indexOf(" -> ")).trim());
                String newPath = normalizePath(rawPath.substring(rawPath.indexOf(" -> ") + 4).trim());
                renamedEntries.add(oldPath + " -> " + newPath);
                modifiedFiles.add(newPath);
                continue;
            }

            String path = normalizePath(rawPath);
            if (isDeletedStatus(x, y)) {
                deletedFiles.add(path);
            } else if (isAddedStatus(x, y)) {
                addedFiles.add(path);
            } else if (isModifiedStatus(x, y) || isRenameStatus(x, y)) {
                modifiedFiles.add(path);
            } else {
                modifiedFiles.add(path);
            }
        }

        return new ParsedGitStatus(addedFiles, modifiedFiles, deletedFiles, renamedEntries);
    }

    private List<String> buildRollbackCommands(Set<String> addedFiles, Set<String> modifiedFiles, Set<String> deletedFiles) {
        LinkedHashSet<String> rollbackCommands = new LinkedHashSet<>();
        for (String file : modifiedFiles) {
            rollbackCommands.add("git restore " + quoteIfNeeded(file));
        }
        for (String file : deletedFiles) {
            rollbackCommands.add("git restore " + quoteIfNeeded(file));
        }
        for (String file : addedFiles) {
            rollbackCommands.add("git rm --cached " + quoteIfNeeded(file));
        }

        List<String> result = new ArrayList<>();
        for (String command : rollbackCommands) {
            if (result.size() >= 20) {
                break;
            }
            result.add(command);
        }
        return result;
    }

    private void addSuggestions(LinkedHashSet<String> suggestions,
                                List<String> techStack,
                                boolean touchedSecurity,
                                boolean touchedSql,
                                boolean modifiedPom,
                                boolean modifiedPackageJson,
                                boolean hasDeletedFiles) {
        Set<String> normalizedTech = new LinkedHashSet<>();
        if (techStack != null) {
            for (String stack : techStack) {
                if (stack != null) {
                    normalizedTech.add(stack.toLowerCase(Locale.ROOT));
                }
            }
        }

        if (normalizedTech.contains("java") || normalizedTech.contains("spring boot") || normalizedTech.contains("maven")) {
            suggestions.add("建议运行 mvn test 或 mvn package 验证后端功能。");
        }
        if (normalizedTech.contains("vue")
                || normalizedTech.contains("react")
                || normalizedTech.contains("vite")
                || normalizedTech.contains("typescript")
                || normalizedTech.contains("node.js")) {
            suggestions.add("建议运行 npm run build 验证前端构建。");
        }
        if (modifiedPom) {
            suggestions.add("建议重新加载 Maven 依赖并运行 mvn dependency:tree 检查依赖冲突。");
        }
        if (modifiedPackageJson) {
            suggestions.add("建议重新安装依赖并检查 lock 文件是否同步更新。");
        }
        if (touchedSecurity) {
            suggestions.add("建议重点验证登录、鉴权、权限拦截和 Token 刷新流程。");
        }
        if (touchedSql) {
            suggestions.add("建议在测试库执行 SQL 并确认回滚方案。");
        }
        if (hasDeletedFiles) {
            suggestions.add("建议确认删除是否符合预期，必要时使用 git restore 恢复。");
        }
        suggestions.add("建议检查 Git Diff 后再提交代码。");
        suggestions.add("建议将大范围变更拆分为更小的提交。");
        suggestions.add("建议记录本次 AI Agent 修改的目标和影响范围。");
    }

    private boolean containsAny(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDatabaseFile(String filePathLower) {
        return filePathLower.endsWith(".sql")
                || filePathLower.contains("migration")
                || filePathLower.contains("/db/")
                || filePathLower.contains("\\db\\")
                || filePathLower.contains("sql");
    }

    private boolean isDeployFile(String filePathLower) {
        return containsAny(filePathLower, DEPLOY_KEYWORDS);
    }

    private String fileName(String pathLower) {
        int index = pathLower.lastIndexOf('/');
        return index >= 0 ? pathLower.substring(index + 1) : pathLower;
    }

    private boolean isAddedStatus(char x, char y) {
        return x == 'A' || y == 'A';
    }

    private boolean isDeletedStatus(char x, char y) {
        return x == 'D' || y == 'D';
    }

    private boolean isModifiedStatus(char x, char y) {
        return x == 'M' || y == 'M' || x == 'T' || y == 'T' || x == 'C' || y == 'C' || x == 'U' || y == 'U';
    }

    private boolean isRenameStatus(char x, char y) {
        return x == 'R' || y == 'R';
    }

    private String normalizePath(String rawPath) {
        return rawPath.replace('\\', '/');
    }

    private String quoteIfNeeded(String filePath) {
        if (filePath.contains(" ")) {
            return "\"" + filePath.replace("\"", "\\\"") + "\"";
        }
        return filePath;
    }

    private static class ParsedGitStatus {

        private final LinkedHashSet<String> addedFiles;
        private final LinkedHashSet<String> modifiedFiles;
        private final LinkedHashSet<String> deletedFiles;
        private final LinkedHashSet<String> renamedEntries;

        private ParsedGitStatus(LinkedHashSet<String> addedFiles,
                                LinkedHashSet<String> modifiedFiles,
                                LinkedHashSet<String> deletedFiles,
                                LinkedHashSet<String> renamedEntries) {
            this.addedFiles = addedFiles;
            this.modifiedFiles = modifiedFiles;
            this.deletedFiles = deletedFiles;
            this.renamedEntries = renamedEntries;
        }
    }

    @lombok.Builder
    @lombok.Getter
    public static class GitDiffAuditResult {

        private final int changedFileCount;
        private final List<String> addedFiles;
        private final List<String> modifiedFiles;
        private final List<String> deletedFiles;
        private final RiskLevel riskLevel;
        private final List<String> riskItems;
        private final List<String> suggestions;
        private final List<String> rollbackCommands;
    }
}

package com.agentguard.detector;

import com.agentguard.common.enums.RiskLevel;
import com.agentguard.scanner.ProjectScanner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ProjectRiskDetector {

    private static final Set<String> MEDIUM_RISK_FILE_NAMES = Set.of(
            ".env", "application-prod.yml", "application-prod.yaml", "docker-compose.prod.yml"
    );

    private static final Set<String> HIGH_RISK_FILE_NAMES = Set.of(
            "id_rsa", "id_rsa.pub"
    );

    private static final Set<String> HIGH_RISK_SUFFIXES = Set.of(".pem", ".key", ".p12", ".jks");

    public ProjectRiskAssessment assess(ProjectScanner.ProjectScanContext context, List<String> sensitiveFiles) {
        List<String> safeSensitiveFiles = sensitiveFiles == null ? List.of() : sensitiveFiles;
        RiskLevel riskLevel = RiskLevel.LOW;

        if (!safeSensitiveFiles.isEmpty()) {
            riskLevel = riskLevel.max(RiskLevel.MEDIUM);
        }
        if (containsMediumRiskFiles(safeSensitiveFiles)) {
            riskLevel = riskLevel.max(RiskLevel.MEDIUM);
        }
        if (containsHighRiskFiles(safeSensitiveFiles)) {
            riskLevel = riskLevel.max(RiskLevel.HIGH);
        }
        if (!safeSensitiveFiles.isEmpty() && !context.isHasGit()) {
            riskLevel = riskLevel.max(RiskLevel.HIGH);
        }

        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        if (!context.isHasAgentsMd()) {
            suggestions.add("建议生成 AGENTS.md 以提升 AI Coding Agent 的项目理解能力。");
        }
        if (!safeSensitiveFiles.isEmpty()) {
            suggestions.add("检测到敏感配置文件，建议在 Agent 规则中禁止读取或修改。");
        }
        if (!context.isHasGit()) {
            suggestions.add("当前项目未检测到 Git 仓库，建议先初始化 Git 或创建备份后再让 AI Agent 修改代码。");
        }
        if (context.hasDetectedFile("package.json")) {
            suggestions.add("检测到 Node.js 依赖文件，AI Agent 修改后建议执行 npm install 或 npm run build。");
        }
        if (context.hasDetectedFile("pom.xml")) {
            suggestions.add("检测到 Maven 项目，AI Agent 修改后建议执行 mvn test 或 mvn package。");
        }
        if (context.hasDetectedFile("build.gradle") || context.hasDetectedFile("build.gradle.kts")) {
            suggestions.add("检测到 Gradle 项目，AI Agent 修改后建议执行 gradle test 或 gradle build。");
        }
        if (context.hasDetectedFile("requirements.txt") || context.hasDetectedFile("pyproject.toml")
                || context.hasDetectedFile("setup.py") || context.hasDetectedFile("pipfile")) {
            suggestions.add("检测到 Python 项目，AI Agent 修改后建议执行 pip install 或 python -m pytest。");
        }
        if (context.hasDetectedFile("go.mod")) {
            suggestions.add("检测到 Go 项目，AI Agent 修改后建议执行 go build 或 go test。");
        }
        if (context.hasDetectedFile("cargo.toml")) {
            suggestions.add("检测到 Rust 项目，AI Agent 修改后建议执行 cargo build 或 cargo test。");
        }
        if (context.hasDetectedFile("composer.json")) {
            suggestions.add("检测到 PHP 项目，AI Agent 修改后建议执行 composer install 或 phpunit。");
        }
        if (context.hasDetectedFile("gemfile")) {
            suggestions.add("检测到 Ruby 项目，AI Agent 修改后建议执行 bundle install 或 rails test。");
        }
        return new ProjectRiskAssessment(riskLevel.name(), new ArrayList<>(suggestions));
    }

    private boolean containsMediumRiskFiles(List<String> sensitiveFiles) {
        for (String sensitiveFile : sensitiveFiles) {
            String fileNameLower = extractFileName(sensitiveFile);
            if (MEDIUM_RISK_FILE_NAMES.contains(fileNameLower)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsHighRiskFiles(List<String> sensitiveFiles) {
        for (String sensitiveFile : sensitiveFiles) {
            String fileNameLower = extractFileName(sensitiveFile);
            if (HIGH_RISK_FILE_NAMES.contains(fileNameLower)) {
                return true;
            }
            for (String suffix : HIGH_RISK_SUFFIXES) {
                if (fileNameLower.endsWith(suffix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String extractFileName(String path) {
        String pathLower = path == null ? "" : path.toLowerCase(Locale.ROOT).replace('\\', '/');
        int separatorIndex = pathLower.lastIndexOf('/');
        return separatorIndex >= 0 ? pathLower.substring(separatorIndex + 1) : pathLower;
    }

    public static class ProjectRiskAssessment {

        private final String riskLevel;
        private final List<String> suggestions;

        public ProjectRiskAssessment(String riskLevel, List<String> suggestions) {
            this.riskLevel = riskLevel;
            this.suggestions = suggestions;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public List<String> getSuggestions() {
            return suggestions;
        }
    }
}

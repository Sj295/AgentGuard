package com.agentguard.generator.impl;

import com.agentguard.entity.ProjectInfo;
import com.agentguard.generator.AgentRuleGenerateContext;
import com.agentguard.generator.AgentRuleGenerator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class AbstractAgentRuleGenerator implements AgentRuleGenerator {

    @Override
    public String generate(AgentRuleGenerateContext context) {
        StringBuilder builder = new StringBuilder();
        appendTitle(builder);
        appendAgentSpecificFocus(builder);
        appendProjectOverview(builder, context);
        appendBuildAndRunCommands(builder, context);
        appendSafeWorkingRules(builder);
        appendSensitiveFiles(builder, context.getSensitiveFiles());
        appendProjectSpecificNotes(builder, context.getSuggestions());
        appendRecommendedWorkflow(builder);
        return builder.toString();
    }

    protected abstract void appendTitle(StringBuilder builder);

    protected abstract void appendAgentSpecificFocus(StringBuilder builder);

    protected void appendProjectOverview(StringBuilder builder, AgentRuleGenerateContext context) {
        ProjectInfo projectInfo = context.getProjectInfo();
        builder.append("## Project Overview\n\n")
                .append("- Project Name: ").append(safe(projectInfo.getProjectName())).append('\n')
                .append("- Project Path: ").append(safe(projectInfo.getProjectPath())).append('\n')
                .append("- Project Type: ").append(safe(projectInfo.getProjectType())).append('\n')
                .append("- Tech Stack: ").append(formatList(context.getTechStack())).append('\n')
                .append("- Git Repository: ").append(Boolean.TRUE.equals(projectInfo.getHasGit()) ? "Yes" : "No").append('\n')
                .append("- Current Risk Level: ").append(safe(context.getRiskLevel())).append("\n\n");
    }

    protected void appendBuildAndRunCommands(StringBuilder builder, AgentRuleGenerateContext context) {
        Set<String> commands = collectBuildCommands(context);
        builder.append("## Build and Run Commands\n\n");
        if (commands.isEmpty()) {
            builder.append("- No specific command detected. Use project-specific build/test commands.\n\n");
            return;
        }
        for (String command : commands) {
            builder.append("- `").append(command).append("`\n");
        }
        builder.append('\n');
    }

    protected void appendSafeWorkingRules(StringBuilder builder) {
        builder.append("## Safe Working Rules\n\n")
                .append("- Do not modify .env files.\n")
                .append("- Do not print secrets, tokens, API keys, or private keys.\n")
                .append("- Do not delete files without explicit confirmation.\n")
                .append("- Do not run rm -rf, git reset --hard, git clean -fd, or docker system prune without confirmation.\n")
                .append("- Before changing API contracts, check both frontend and backend usage.\n")
                .append("- After code changes, summarize modified files and potential risks.\n")
                .append("- Prefer small, incremental changes over large rewrites.\n\n");
    }

    protected void appendSensitiveFiles(StringBuilder builder, List<String> sensitiveFiles) {
        builder.append("## Sensitive Files\n\n");
        if (sensitiveFiles == null || sensitiveFiles.isEmpty()) {
            builder.append("- No sensitive file detected in latest scan.\n\n");
            return;
        }
        for (String sensitiveFile : sensitiveFiles) {
            builder.append("- ").append(sensitiveFile).append('\n');
        }
        builder.append("\nThese files should not be read, modified, printed, or committed unless explicitly approved.\n\n");
    }

    protected void appendProjectSpecificNotes(StringBuilder builder, List<String> suggestions) {
        builder.append("## Project-specific Notes\n\n");
        if (suggestions == null || suggestions.isEmpty()) {
            builder.append("- No additional project notes from latest scan.\n\n");
            return;
        }
        for (String suggestion : suggestions) {
            builder.append("- ").append(suggestion).append('\n');
        }
        builder.append('\n');
    }

    protected void appendRecommendedWorkflow(StringBuilder builder) {
        builder.append("## Recommended Workflow\n\n")
                .append("- Create or switch to a Git branch before large changes.\n")
                .append("- Run tests or build commands after modifications.\n")
                .append("- Review git diff before committing.\n")
                .append("- Ask for confirmation before destructive commands.\n")
                .append("- Keep changes scoped to the requested task.\n");
    }

    private Set<String> collectBuildCommands(AgentRuleGenerateContext context) {
        List<String> techStack = context.getTechStack() == null ? List.of() : context.getTechStack();
        List<String> detectedFiles = context.getDetectedFiles() == null ? List.of() : context.getDetectedFiles();
        Set<String> normalizedTech = new LinkedHashSet<>();
        for (String stack : techStack) {
            normalizedTech.add(stack.toLowerCase(Locale.ROOT));
        }
        Set<String> normalizedFiles = new LinkedHashSet<>();
        for (String file : detectedFiles) {
            normalizedFiles.add(file.toLowerCase(Locale.ROOT));
        }

        LinkedHashSet<String> commands = new LinkedHashSet<>();
        if (normalizedTech.contains("maven") || normalizedFiles.contains("pom.xml")) {
            commands.add("mvn test");
            commands.add("mvn package");
            commands.add("mvn spring-boot:run");
        }
        if (normalizedTech.contains("gradle") || normalizedFiles.contains("build.gradle")) {
            commands.add("./gradlew test");
            commands.add("./gradlew build");
        }
        boolean hasFrontend = normalizedTech.contains("node.js")
                || normalizedTech.contains("vue")
                || normalizedTech.contains("react")
                || normalizedTech.contains("vite")
                || normalizedFiles.contains("package.json")
                || normalizedFiles.contains("vite.config.ts")
                || normalizedFiles.contains("vite.config.js");
        if (hasFrontend) {
            commands.add("npm install");
            commands.add("npm run dev");
            commands.add("npm run build");
        }
        if (normalizedTech.contains("docker")
                || normalizedFiles.contains("dockerfile")
                || normalizedFiles.contains("docker-compose.yml")) {
            commands.add("docker compose up -d");
        }
        return commands;
    }

    private String safe(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }

    private String formatList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "N/A";
        }
        return String.join(", ", new ArrayList<>(values));
    }
}

package com.agentguard.generator;

import com.agentguard.entity.AgentRule;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.vo.AgentRuleWriteVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@Component
public class AgentRuleFileWriter {

    private static final Logger log = LoggerFactory.getLogger(AgentRuleFileWriter.class);

    private static final Set<String> ALLOWED_FILE_NAMES = Set.of(
            "AGENTS.md",
            "CLAUDE.md",
            ".cursor/rules/agentguard.mdc"
    );

    private static final DateTimeFormatter BACKUP_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public AgentRuleWriteVO write(AgentRule rule, ProjectInfo project, boolean overwrite, boolean backup) {
        validateRuleFileName(rule.getFileName());
        Path projectRoot = validateProjectPath(project.getProjectPath());
        Path targetPath = resolveTargetPath(projectRoot, rule.getFileName());
        validateTargetInsideProject(targetPath, projectRoot);

        AgentRuleWriteVO vo = new AgentRuleWriteVO();
        vo.setRuleId(rule.getId());
        vo.setProjectId(rule.getProjectId());
        vo.setAgentType(rule.getAgentType());
        vo.setFileName(rule.getFileName());
        vo.setTargetPath(targetPath.toString().replace('\\', '/'));

        boolean targetExists = Files.exists(targetPath);
        vo.setOverwritten(false);
        vo.setBackupPath(null);

        if (targetExists && !overwrite) {
            throw new IllegalArgumentException("目标文件已存在，请开启 overwrite。");
        }

        if (targetExists && backup) {
            String backupPath = backupExistingFile(projectRoot, rule.getFileName());
            vo.setBackupPath(backupPath);
        }

        if (targetExists) {
            vo.setOverwritten(true);
        }

        ensureParentDirectory(targetPath);
        writeContent(targetPath, rule.getContent());

        vo.setWritten(true);
        vo.setMessage("规则文件已成功写入项目目录");
        log.info("Agent rule file written: ruleId={}, targetPath={}", rule.getId(), targetPath);
        return vo;
    }

    private void validateRuleFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("规则文件名不能为空");
        }
        if (fileName.contains("..")) {
            throw new IllegalArgumentException("文件名不允许包含路径穿越字符");
        }
        if (!ALLOWED_FILE_NAMES.contains(fileName)) {
            throw new IllegalArgumentException("不允许写入该文件: " + fileName + "，仅允许: " + ALLOWED_FILE_NAMES);
        }
    }

    private Path validateProjectPath(String projectPath) {
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException("项目路径不能为空");
        }
        Path path = Paths.get(projectPath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("项目路径不存在: " + projectPath);
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("项目路径不是目录: " + projectPath);
        }
        return path.toAbsolutePath().normalize();
    }

    private Path resolveTargetPath(Path projectRoot, String fileName) {
        return projectRoot.resolve(fileName).normalize();
    }

    private void validateTargetInsideProject(Path targetPath, Path projectRoot) {
        if (!targetPath.startsWith(projectRoot)) {
            throw new IllegalArgumentException("目标路径不在项目目录内，写入被拒绝");
        }
    }

    private String backupExistingFile(Path projectRoot, String fileName) {
        Path backupDir = projectRoot.resolve(".agentguard").resolve("backups");
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            throw new RuntimeException("创建备份目录失败: " + backupDir, e);
        }

        String timestamp = LocalDateTime.now().format(BACKUP_TIME_FORMAT);
        String baseName = fileName.replace("/", "_").replace("\\", "_");
        String backupFileName = baseName + "." + timestamp + ".bak";
        Path backupPath = backupDir.resolve(backupFileName);

        Path sourcePath = projectRoot.resolve(fileName);
        try {
            Files.copy(sourcePath, backupPath);
        } catch (IOException e) {
            throw new RuntimeException("备份文件失败: " + sourcePath + " -> " + backupPath, e);
        }

        log.info("Backed up existing file: {} -> {}", sourcePath, backupPath);
        return backupPath.toString().replace('\\', '/');
    }

    private void ensureParentDirectory(Path targetPath) {
        Path parent = targetPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new RuntimeException("创建父目录失败: " + parent, e);
            }
        }
    }

    private void writeContent(Path targetPath, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("规则内容为空，无法写入");
        }
        try {
            Files.writeString(targetPath, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + targetPath, e);
        }
    }
}

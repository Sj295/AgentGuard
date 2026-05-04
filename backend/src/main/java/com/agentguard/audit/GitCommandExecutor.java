package com.agentguard.audit;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class GitCommandExecutor {

    private static final long COMMAND_TIMEOUT_SECONDS = 10L;

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "git status --porcelain",
            "git diff --name-only",
            "git diff --cached --name-only",
            "git ls-files --deleted",
            "git ls-files --others --exclude-standard"
    );

    public CommandResult execute(Path projectPath, List<String> command) {
        validateAllowedCommand(command);
        validateGitRepository(projectPath);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(projectPath.toFile());
        processBuilder.redirectErrorStream(false);

        ExecutorService streamPool = Executors.newFixedThreadPool(2);
        Process process = null;
        try {
            Process runningProcess = processBuilder.start();
            process = runningProcess;
            final Process finalProcess = runningProcess;
            Future<String> stdoutFuture = streamPool.submit(() -> readStream(finalProcess.getInputStream()));
            Future<String> stderrFuture = streamPool.submit(() -> readStream(finalProcess.getErrorStream()));

            boolean completed = finalProcess.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                finalProcess.destroyForcibly();
                throw new RuntimeException("Git command timeout: " + String.join(" ", command));
            }

            String stdout = stdoutFuture.get();
            String stderr = stderrFuture.get();
            int exitCode = finalProcess.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("Git command failed: " + String.join(" ", command) + ", error: " + stderr);
            }
            return new CommandResult(stdout, stderr, exitCode);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to execute git command: " + String.join(" ", command), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Git command interrupted: " + String.join(" ", command), exception);
        } catch (ExecutionException exception) {
            throw new RuntimeException("Failed to read git command output: " + String.join(" ", command), exception);
        } finally {
            streamPool.shutdownNow();
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    public void validateGitRepository(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            throw new IllegalArgumentException("项目路径无效，无法执行 Git 命令：" + projectPath);
        }
        Path gitPath = projectPath.resolve(".git");
        if (!Files.exists(gitPath) || !Files.isDirectory(gitPath)) {
            throw new IllegalArgumentException("当前项目不是 Git 仓库，无法执行 Git Diff 审计。");
        }
    }

    private void validateAllowedCommand(List<String> command) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("Git command cannot be empty");
        }
        String normalized = String.join(" ", command).trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_COMMANDS.contains(normalized)) {
            throw new IllegalArgumentException("Forbidden git command: " + String.join(" ", command));
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }

    public static class CommandResult {

        private final String stdout;
        private final String stderr;
        private final int exitCode;

        public CommandResult(String stdout, String stderr, int exitCode) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.exitCode = exitCode;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public int getExitCode() {
            return exitCode;
        }
    }
}

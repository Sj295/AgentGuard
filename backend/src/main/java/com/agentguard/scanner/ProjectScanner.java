package com.agentguard.scanner;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class ProjectScanner {

    private static final int MAX_FILE_READ_BYTES = 200 * 1024;

    private static final Set<String> SKIP_DIR_NAMES = Set.of(
            ".git", "node_modules", "target", "dist", "build", ".idea", ".vscode", "logs", "tmp",
            "storage", "__pycache__", ".pytest_cache", ".mvn", ".tox", ".mypy_cache",
            "vendor", "venv", ".venv", "env", ".env-dir",
            "bin", "obj", "Debug", "Release",
            ".next", ".nuxt", ".output", ".cache"
    );

    private static final Set<String> KEY_FILE_NAMES = Set.of(
            // Java / JVM
            "pom.xml", "build.gradle", "settings.gradle", "build.gradle.kts",
            // Node.js / Frontend
            "package.json", "vite.config.ts", "vite.config.js", "tsconfig.json",
            // Python
            "requirements.txt", "pyproject.toml", "setup.py", "setup.cfg", "pipfile",
            // Go
            "go.mod",
            // Rust
            "cargo.toml",
            // PHP
            "composer.json",
            // Ruby
            "gemfile",
            // C / C++
            "cmakelists.txt", "makefile",
            // Swift
            "package.swift",
            // Docker / Infra
            "dockerfile", "docker-compose.yml", "docker-compose.yaml",
            // Docs
            "readme.md",
            // Sensitive
            ".env",
            // AI agents
            "agents.md", "claude.md",
            // Spring config
            "application.yml", "application.yaml", "application-dev.yml", "application-prod.yml"
    );

    private static final Set<String> KEY_FILE_EXTENSIONS = Set.of(
            ".csproj", ".sln"
    );

    private static final Map<String, String> KEY_FILE_DISPLAY = Map.ofEntries(
            // Java / JVM
            Map.entry("pom.xml", "pom.xml"),
            Map.entry("build.gradle", "build.gradle"),
            Map.entry("settings.gradle", "settings.gradle"),
            Map.entry("build.gradle.kts", "build.gradle.kts"),
            // Node.js / Frontend
            Map.entry("package.json", "package.json"),
            Map.entry("vite.config.ts", "vite.config.ts"),
            Map.entry("vite.config.js", "vite.config.js"),
            Map.entry("tsconfig.json", "tsconfig.json"),
            // Python
            Map.entry("requirements.txt", "requirements.txt"),
            Map.entry("pyproject.toml", "pyproject.toml"),
            Map.entry("setup.py", "setup.py"),
            Map.entry("setup.cfg", "setup.cfg"),
            Map.entry("pipfile", "Pipfile"),
            // Go
            Map.entry("go.mod", "go.mod"),
            // Rust
            Map.entry("cargo.toml", "Cargo.toml"),
            // PHP
            Map.entry("composer.json", "composer.json"),
            // Ruby
            Map.entry("gemfile", "Gemfile"),
            // C / C++
            Map.entry("cmakelists.txt", "CMakeLists.txt"),
            Map.entry("makefile", "Makefile"),
            // Swift
            Map.entry("package.swift", "Package.swift"),
            // Docker / Infra
            Map.entry("dockerfile", "Dockerfile"),
            Map.entry("docker-compose.yml", "docker-compose.yml"),
            Map.entry("docker-compose.yaml", "docker-compose.yaml"),
            // Docs
            Map.entry("readme.md", "README.md"),
            // Sensitive
            Map.entry(".env", ".env"),
            // AI agents
            Map.entry("agents.md", "AGENTS.md"),
            Map.entry("claude.md", "CLAUDE.md"),
            // Spring config
            Map.entry("application.yml", "application.yml"),
            Map.entry("application.yaml", "application.yaml"),
            Map.entry("application-dev.yml", "application-dev.yml"),
            Map.entry("application-prod.yml", "application-prod.yml"),
            Map.entry(".cursor/rules", ".cursor/rules"),
            // .NET
            Map.entry(".csproj", "*.csproj"),
            Map.entry(".sln", "*.sln")
    );

    private static final Set<String> SAMPLE_CONTENT_FILE_NAMES = Set.of(
            "pom.xml", "package.json", "application.yml", "application.yaml",
            "requirements.txt", "pyproject.toml", "go.mod", "cargo.toml",
            "composer.json", "gemfile", "setup.py", "setup.cfg", "pipfile"
    );

    public ProjectScanContext scan(String projectPath) {
        Path rootPath = normalizeAndValidatePath(projectPath);
        String normalizedProjectPath = normalizePathForStorage(rootPath.toString());

        long[] fileCount = {0L};
        long[] directoryCount = {0L};
        boolean[] hasGit = {Files.isDirectory(rootPath.resolve(".git"))};
        boolean[] hasAgentsMd = {false};
        LinkedHashSet<String> detectedFiles = new LinkedHashSet<>();
        LinkedHashSet<String> detectedMarkers = new LinkedHashSet<>();
        List<String> allFilePaths = new ArrayList<>();
        Map<String, String> sampleContents = new HashMap<>();

        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (rootPath.equals(dir)) {
                        return FileVisitResult.CONTINUE;
                    }

                    String dirNameLower = dir.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (".git".equals(dirNameLower)) {
                        hasGit[0] = true;
                    }
                    if (SKIP_DIR_NAMES.contains(dirNameLower)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    // Skip nested git repositories (treat as separate project boundaries)
                    if (Files.isDirectory(dir.resolve(".git"))) {
                        hasGit[0] = true;
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    directoryCount[0]++;
                    String relativePathLower = normalizeRelativePath(rootPath, dir).toLowerCase(Locale.ROOT);
                    if (".cursor/rules".equals(relativePathLower)) {
                        addDetectedFile(detectedFiles, detectedMarkers, ".cursor/rules");
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    fileCount[0]++;
                    String relativePath = normalizeRelativePath(rootPath, file);
                    String relativePathLower = relativePath.toLowerCase(Locale.ROOT);
                    String fileNameLower = file.getFileName().toString().toLowerCase(Locale.ROOT);
                    allFilePaths.add(relativePath);

                    if ("agents.md".equals(fileNameLower)) {
                        hasAgentsMd[0] = true;
                    }
                    if (KEY_FILE_NAMES.contains(fileNameLower)) {
                        addDetectedFile(detectedFiles, detectedMarkers, fileNameLower);
                    }
                    int dotIndex = fileNameLower.lastIndexOf('.');
                    if (dotIndex > 0) {
                        String ext = fileNameLower.substring(dotIndex);
                        if (KEY_FILE_EXTENSIONS.contains(ext)) {
                            addDetectedFile(detectedFiles, detectedMarkers, ext);
                        }
                    }
                    if (relativePathLower.startsWith(".cursor/rules/")) {
                        addDetectedFile(detectedFiles, detectedMarkers, ".cursor/rules");
                    }
                    if (SAMPLE_CONTENT_FILE_NAMES.contains(fileNameLower) && !sampleContents.containsKey(fileNameLower)) {
                        sampleContents.put(fileNameLower, readFileHead(file, MAX_FILE_READ_BYTES));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            throw new RuntimeException("Failed to scan project path: " + exception.getMessage(), exception);
        }

        if (!hasAgentsMd[0]) {
            hasAgentsMd[0] = Files.exists(rootPath.resolve("AGENTS.md")) || Files.exists(rootPath.resolve("agents.md"));
            if (hasAgentsMd[0]) {
                addDetectedFile(detectedFiles, detectedMarkers, "agents.md");
            }
        }

        return new ProjectScanContext(
                normalizedProjectPath,
                fileCount[0],
                directoryCount[0],
                hasGit[0],
                hasAgentsMd[0],
                new ArrayList<>(detectedFiles),
                new LinkedHashSet<>(detectedMarkers),
                allFilePaths,
                sampleContents
        );
    }

    public String normalizeProjectPath(String projectPath) {
        return normalizePathForStorage(normalizeAndValidatePath(projectPath).toString());
    }

    private void addDetectedFile(LinkedHashSet<String> detectedFiles, LinkedHashSet<String> detectedMarkers, String marker) {
        String normalizedMarker = marker.toLowerCase(Locale.ROOT);
        detectedMarkers.add(normalizedMarker);
        detectedFiles.add(KEY_FILE_DISPLAY.getOrDefault(normalizedMarker, marker));
    }

    private String readFileHead(Path filePath, int maxBytes) {
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            byte[] bytes = inputStream.readNBytes(maxBytes);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to read file: " + filePath, exception);
        }
    }

    private Path normalizeAndValidatePath(String projectPath) {
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException("Project path cannot be blank");
        }
        Path path;
        try {
            path = Paths.get(projectPath).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("Invalid project path: " + projectPath);
        }
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Project path does not exist: " + projectPath);
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalArgumentException("Project path is not a directory: " + projectPath);
        }
        return path;
    }

    private String normalizeRelativePath(Path rootPath, Path currentPath) {
        return rootPath.relativize(currentPath).toString().replace('\\', '/');
    }

    private String normalizePathForStorage(String rawPath) {
        return rawPath.replace('\\', '/');
    }

    public static class ProjectScanContext {

        private final String normalizedProjectPath;
        private final long fileCount;
        private final long directoryCount;
        private final boolean hasGit;
        private final boolean hasAgentsMd;
        private final List<String> detectedFiles;
        private final Set<String> detectedMarkers;
        private final List<String> allFilePaths;
        private final Map<String, String> sampleContents;

        public ProjectScanContext(String normalizedProjectPath,
                                  long fileCount,
                                  long directoryCount,
                                  boolean hasGit,
                                  boolean hasAgentsMd,
                                  List<String> detectedFiles,
                                  Set<String> detectedMarkers,
                                  List<String> allFilePaths,
                                  Map<String, String> sampleContents) {
            this.normalizedProjectPath = normalizedProjectPath;
            this.fileCount = fileCount;
            this.directoryCount = directoryCount;
            this.hasGit = hasGit;
            this.hasAgentsMd = hasAgentsMd;
            this.detectedFiles = Collections.unmodifiableList(new ArrayList<>(detectedFiles));
            this.detectedMarkers = Collections.unmodifiableSet(new LinkedHashSet<>(detectedMarkers));
            this.allFilePaths = Collections.unmodifiableList(new ArrayList<>(allFilePaths));
            this.sampleContents = Collections.unmodifiableMap(new HashMap<>(sampleContents));
        }

        public String getNormalizedProjectPath() {
            return normalizedProjectPath;
        }

        public long getFileCount() {
            return fileCount;
        }

        public long getDirectoryCount() {
            return directoryCount;
        }

        public boolean isHasGit() {
            return hasGit;
        }

        public boolean isHasAgentsMd() {
            return hasAgentsMd;
        }

        public List<String> getDetectedFiles() {
            return detectedFiles;
        }

        public List<String> getAllFilePaths() {
            return allFilePaths;
        }

        public boolean hasDetectedFile(String marker) {
            return marker != null && detectedMarkers.contains(marker.toLowerCase(Locale.ROOT));
        }

        public String getSampleContent(String fileName) {
            if (fileName == null || fileName.isBlank()) {
                return "";
            }
            return sampleContents.getOrDefault(fileName.toLowerCase(Locale.ROOT), "");
        }
    }
}

package com.agentguard.scanner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProjectScannerTest {

    private ProjectScanner scanner;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        scanner = new ProjectScanner();
    }

    @Test
    void scan_basicProject_shouldDetectFilesAndDirs() throws IOException {
        Files.createFile(tempDir.resolve("README.md"));
        Files.createFile(tempDir.resolve("package.json"));
        Path src = Files.createDirectories(tempDir.resolve("src"));
        Files.createFile(src.resolve("index.js"));

        ProjectScanner.ProjectScanContext ctx = scanner.scan(tempDir.toString());

        assertEquals(3, ctx.getFileCount());
        assertEquals(1, ctx.getDirectoryCount());
        assertTrue(ctx.getDetectedFiles().contains("README.md"));
        assertTrue(ctx.getDetectedFiles().contains("package.json"));
    }

    @Test
    void scan_shouldSkipNodeModules() throws IOException {
        Files.createFile(tempDir.resolve("index.js"));
        Path nm = Files.createDirectories(tempDir.resolve("node_modules"));
        Files.createFile(nm.resolve("lodash.js"));
        Files.createDirectories(nm.resolve("lodash"));

        ProjectScanner.ProjectScanContext ctx = scanner.scan(tempDir.toString());

        assertEquals(1, ctx.getFileCount());
        assertEquals(0, ctx.getDirectoryCount());
    }

    @Test
    void scan_shouldSkipTargetAndDist() throws IOException {
        Files.createFile(tempDir.resolve("pom.xml"));
        Path target = Files.createDirectories(tempDir.resolve("target"));
        Files.createFile(target.resolve("app.jar"));
        Path dist = Files.createDirectories(tempDir.resolve("dist"));
        Files.createFile(dist.resolve("index.html"));

        ProjectScanner.ProjectScanContext ctx = scanner.scan(tempDir.toString());

        assertEquals(1, ctx.getFileCount());
        assertTrue(ctx.getDetectedFiles().contains("pom.xml"));
    }

    @Test
    void scan_shouldDetectGitAtRoot() throws IOException {
        Files.createDirectories(tempDir.resolve(".git"));
        Files.createFile(tempDir.resolve("README.md"));

        ProjectScanner.ProjectScanContext ctx = scanner.scan(tempDir.toString());

        assertTrue(ctx.isHasGit());
    }

    @Test
    void scan_noGitDir_shouldSetHasGitFalse() throws IOException {
        Files.createFile(tempDir.resolve("README.md"));

        ProjectScanner.ProjectScanContext ctx = scanner.scan(tempDir.toString());

        assertFalse(ctx.isHasGit());
    }

    @Test
    void scan_shouldDetectAgentsMd() throws IOException {
        Files.createFile(tempDir.resolve("AGENTS.md"));
        Files.createFile(tempDir.resolve("README.md"));

        ProjectScanner.ProjectScanContext ctx = scanner.scan(tempDir.toString());

        assertTrue(ctx.isHasAgentsMd());
        assertTrue(ctx.getDetectedFiles().contains("AGENTS.md"));
    }

    @Test
    void scan_shouldSkipNestedGitRepos() throws IOException {
        // Root project files
        Files.createFile(tempDir.resolve("package.json"));
        Files.createFile(tempDir.resolve("README.md"));

        // Nested repo with its own .git
        Path nestedRepo = Files.createDirectories(tempDir.resolve("libs/some-repo"));
        Files.createDirectories(nestedRepo.resolve(".git"));
        Files.createFile(nestedRepo.resolve("pom.xml"));
        Files.createFile(nestedRepo.resolve("Dockerfile"));
        Path nestedSrc = Files.createDirectories(nestedRepo.resolve("src/main/java"));
        Files.createFile(nestedSrc.resolve("App.java"));

        ProjectScanner.ProjectScanContext ctx = scanner.scan(tempDir.toString());

        // Should only count root-level files, not nested repo contents
        assertEquals(2, ctx.getFileCount()); // package.json, README.md
        assertEquals(1, ctx.getDirectoryCount()); // libs/ (but not libs/some-repo since it's skipped)
        // hasGit should still be true because nested repo has .git
        assertTrue(ctx.isHasGit());
        // pom.xml and Dockerfile from nested repo should NOT be detected
        assertFalse(ctx.getDetectedFiles().contains("pom.xml"));
        assertFalse(ctx.getDetectedFiles().contains("Dockerfile"));
        assertTrue(ctx.getDetectedFiles().contains("package.json"));
    }

    @Test
    void scan_shouldSkipStorageAndPycache() throws IOException {
        Files.createFile(tempDir.resolve("app.py"));
        Path storage = Files.createDirectories(tempDir.resolve("storage"));
        Files.createFile(storage.resolve("data.db"));
        Path cache = Files.createDirectories(tempDir.resolve("__pycache__"));
        Files.createFile(cache.resolve("app.cpython-310.pyc"));

        ProjectScanner.ProjectScanContext ctx = scanner.scan(tempDir.toString());

        assertEquals(1, ctx.getFileCount());
        assertEquals(0, ctx.getDirectoryCount());
    }

    @Test
    void scan_shouldReadSampleContent() throws IOException {
        String pomContent = "<?xml version=\"1.0\"?><project><dependency>spring-boot</dependency></project>";
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        ProjectScanner.ProjectScanContext ctx = scanner.scan(tempDir.toString());

        assertTrue(ctx.getSampleContent("pom.xml").contains("spring-boot"));
    }

    @Test
    void scan_shouldDetectViteConfig() throws IOException {
        Files.createFile(tempDir.resolve("vite.config.ts"));
        Files.createFile(tempDir.resolve("package.json"));

        ProjectScanner.ProjectScanContext ctx = scanner.scan(tempDir.toString());

        assertTrue(ctx.getDetectedFiles().contains("vite.config.ts"));
    }

    @Test
    void scan_nullPath_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> scanner.scan(null));
    }

    @Test
    void scan_blankPath_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> scanner.scan("  "));
    }

    @Test
    void scan_nonExistentPath_shouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> scanner.scan("/nonexistent/path"));
    }
}

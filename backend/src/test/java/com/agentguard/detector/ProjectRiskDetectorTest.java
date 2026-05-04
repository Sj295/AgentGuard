package com.agentguard.detector;

import com.agentguard.scanner.ProjectScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRiskDetectorTest {

    private ProjectRiskDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ProjectRiskDetector();
    }

    @Test
    void assess_noSensitiveFiles_shouldReturnLow() {
        ProjectScanner.ProjectScanContext ctx = buildContext(true, true);
        List<String> sensitiveFiles = List.of();

        ProjectRiskDetector.ProjectRiskAssessment result = detector.assess(ctx, sensitiveFiles);

        assertEquals("LOW", result.getRiskLevel());
    }

    @Test
    void assess_hasEnvFile_shouldReturnMedium() {
        ProjectScanner.ProjectScanContext ctx = buildContext(true, false);
        List<String> sensitiveFiles = List.of("backend/.env");

        ProjectRiskDetector.ProjectRiskAssessment result = detector.assess(ctx, sensitiveFiles);

        assertEquals("MEDIUM", result.getRiskLevel());
    }

    @Test
    void assess_hasPemFile_shouldReturnHigh() {
        ProjectScanner.ProjectScanContext ctx = buildContext(true, false);
        List<String> sensitiveFiles = List.of("certs/server.pem");

        ProjectRiskDetector.ProjectRiskAssessment result = detector.assess(ctx, sensitiveFiles);

        assertEquals("HIGH", result.getRiskLevel());
    }

    @Test
    void assess_sensitiveFilesWithoutGit_shouldReturnHigh() {
        ProjectScanner.ProjectScanContext ctx = buildContext(false, false);
        List<String> sensitiveFiles = List.of("backend/.env");

        ProjectRiskDetector.ProjectRiskAssessment result = detector.assess(ctx, sensitiveFiles);

        assertEquals("HIGH", result.getRiskLevel());
    }

    @Test
    void assess_noAgentsMd_shouldSuggest() {
        ProjectScanner.ProjectScanContext ctx = buildContext(true, false);
        List<String> sensitiveFiles = List.of();

        ProjectRiskDetector.ProjectRiskAssessment result = detector.assess(ctx, sensitiveFiles);

        assertTrue(result.getSuggestions().stream().anyMatch(s -> s.contains("AGENTS.md")));
    }

    @Test
    void assess_hasAgentsMd_shouldNotSuggest() {
        ProjectScanner.ProjectScanContext ctx = buildContext(true, true);
        List<String> sensitiveFiles = List.of();

        ProjectRiskDetector.ProjectRiskAssessment result = detector.assess(ctx, sensitiveFiles);

        assertFalse(result.getSuggestions().stream().anyMatch(s -> s.contains("AGENTS.md")));
    }

    @Test
    void assess_noGit_shouldSuggest() {
        ProjectScanner.ProjectScanContext ctx = buildContext(false, false);
        List<String> sensitiveFiles = List.of();

        ProjectRiskDetector.ProjectRiskAssessment result = detector.assess(ctx, sensitiveFiles);

        assertTrue(result.getSuggestions().stream().anyMatch(s -> s.contains("Git")));
    }

    @Test
    void assess_hasPackageJson_shouldSuggestNpm() {
        ProjectScanner.ProjectScanContext ctx = buildContext(true, false, "package.json");
        List<String> sensitiveFiles = List.of();

        ProjectRiskDetector.ProjectRiskAssessment result = detector.assess(ctx, sensitiveFiles);

        assertTrue(result.getSuggestions().stream().anyMatch(s -> s.contains("npm")));
    }

    @Test
    void assess_hasPomXml_shouldSuggestMvn() {
        ProjectScanner.ProjectScanContext ctx = buildContext(true, false, "pom.xml");
        List<String> sensitiveFiles = List.of();

        ProjectRiskDetector.ProjectRiskAssessment result = detector.assess(ctx, sensitiveFiles);

        assertTrue(result.getSuggestions().stream().anyMatch(s -> s.contains("mvn")));
    }

    @Test
    void assess_hasRequirementsTxt_shouldSuggestPip() {
        ProjectScanner.ProjectScanContext ctx = buildContext(true, false, "requirements.txt");
        List<String> sensitiveFiles = List.of();

        ProjectRiskDetector.ProjectRiskAssessment result = detector.assess(ctx, sensitiveFiles);

        assertTrue(result.getSuggestions().stream().anyMatch(s -> s.contains("pip")));
    }

    @Test
    void assess_hasGoMod_shouldSuggestGo() {
        ProjectScanner.ProjectScanContext ctx = buildContext(true, false, "go.mod");
        List<String> sensitiveFiles = List.of();

        ProjectRiskDetector.ProjectRiskAssessment result = detector.assess(ctx, sensitiveFiles);

        assertTrue(result.getSuggestions().stream().anyMatch(s -> s.contains("go build")));
    }

    @Test
    void assess_hasCargoToml_shouldSuggestCargo() {
        ProjectScanner.ProjectScanContext ctx = buildContext(true, false, "cargo.toml");
        List<String> sensitiveFiles = List.of();

        ProjectRiskDetector.ProjectRiskAssessment result = detector.assess(ctx, sensitiveFiles);

        assertTrue(result.getSuggestions().stream().anyMatch(s -> s.contains("cargo")));
    }

    @Test
    void assess_nullSensitiveFiles_shouldHandleGracefully() {
        ProjectScanner.ProjectScanContext ctx = buildContext(true, true);

        ProjectRiskDetector.ProjectRiskAssessment result = detector.assess(ctx, null);

        assertEquals("LOW", result.getRiskLevel());
    }

    private ProjectScanner.ProjectScanContext buildContext(boolean hasGit, boolean hasAgentsMd, String... markers) {
        return new ProjectScanner.ProjectScanContext(
                "/tmp/test",
                10L, 5L,
                hasGit, hasAgentsMd,
                List.of(markers),
                new LinkedHashSet<>(List.of(markers)),
                List.of(),
                Map.of()
        );
    }
}

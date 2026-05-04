package com.agentguard.detector;

import com.agentguard.scanner.ProjectScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TechStackDetectorTest {

    private TechStackDetector detector;

    @BeforeEach
    void setUp() {
        detector = new TechStackDetector();
    }

    // --- Java / JVM ---

    @Test
    void detect_javaMavenSpringBoot_shouldDetectFullStack() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("pom.xml", "package.json"),
                Map.of("pom.xml", "<project><dependency>spring-boot-starter</dependency></project>",
                       "package.json", "{\"dependencies\":{\"vue\":\"3.0\"}}")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertEquals("FULL_STACK", result.getProjectType());
        assertTrue(result.getTechStack().contains("Java"));
        assertTrue(result.getTechStack().contains("Maven"));
        assertTrue(result.getTechStack().contains("Spring Boot"));
        assertTrue(result.getTechStack().contains("Node.js"));
        assertTrue(result.getTechStack().contains("Vue"));
    }

    @Test
    void detect_gradleProject_shouldDetectBackend() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("build.gradle"),
                Map.of()
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertEquals("BACKEND", result.getProjectType());
        assertTrue(result.getTechStack().contains("Java"));
        assertTrue(result.getTechStack().contains("Gradle"));
    }

    // --- Python ---

    @Test
    void detect_pythonFastAPI_shouldDetectBackend() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("requirements.txt"),
                Map.of("requirements.txt", "fastapi==0.115.0\nuvicorn==0.30.0\npydantic==2.8.0")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertEquals("BACKEND", result.getProjectType());
        assertTrue(result.getTechStack().contains("Python"));
        assertTrue(result.getTechStack().contains("FastAPI"));
        assertTrue(result.getTechStack().contains("uvicorn") || result.getTechStack().contains("FastAPI"));
    }

    @Test
    void detect_pythonDjango_shouldDetectBackend() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("pyproject.toml"),
                Map.of("pyproject.toml", "[project]\ndependencies=[\"django>=4.0\"]")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertEquals("BACKEND", result.getProjectType());
        assertTrue(result.getTechStack().contains("Python"));
        assertTrue(result.getTechStack().contains("Django"));
    }

    @Test
    void detect_pythonFlask_shouldDetectBackend() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("requirements.txt"),
                Map.of("requirements.txt", "flask==3.0.0\nrequests==2.31.0")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertEquals("BACKEND", result.getProjectType());
        assertTrue(result.getTechStack().contains("Python"));
        assertTrue(result.getTechStack().contains("Flask"));
    }

    @Test
    void detect_pythonFullStack_shouldDetectFullStack() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("requirements.txt", "package.json"),
                Map.of("requirements.txt", "fastapi==0.115.0",
                       "package.json", "{\"dependencies\":{\"vue\":\"3.5\"}}")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertEquals("FULL_STACK", result.getProjectType());
        assertTrue(result.getTechStack().contains("Python"));
        assertTrue(result.getTechStack().contains("FastAPI"));
        assertTrue(result.getTechStack().contains("Node.js"));
        assertTrue(result.getTechStack().contains("Vue"));
    }

    // --- Go ---

    @Test
    void detect_goWithGin_shouldDetectBackend() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("go.mod"),
                Map.of("go.mod", "module myapp\nrequire github.com/gin-gonic/gin v1.9.0")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertEquals("BACKEND", result.getProjectType());
        assertTrue(result.getTechStack().contains("Go"));
        assertTrue(result.getTechStack().contains("Gin"));
    }

    @Test
    void detect_goPlain_shouldDetectBackend() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("go.mod"),
                Map.of("go.mod", "module myapp\ngo 1.21")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertEquals("BACKEND", result.getProjectType());
        assertTrue(result.getTechStack().contains("Go"));
        assertFalse(result.getTechStack().contains("Gin"));
    }

    // --- Rust ---

    @Test
    void detect_rustActix_shouldDetectBackend() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("cargo.toml"),
                Map.of("cargo.toml", "[package]\nname=\"myapp\"\n[dependencies]\nactix-web = \"4\"")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertEquals("BACKEND", result.getProjectType());
        assertTrue(result.getTechStack().contains("Rust"));
        assertTrue(result.getTechStack().contains("Actix"));
    }

    // --- PHP ---

    @Test
    void detect_phpLaravel_shouldDetectBackend() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("composer.json"),
                Map.of("composer.json", "{\"require\":{\"laravel/framework\":\"^10.0\"}}")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertEquals("BACKEND", result.getProjectType());
        assertTrue(result.getTechStack().contains("PHP"));
        assertTrue(result.getTechStack().contains("Laravel"));
    }

    // --- Ruby ---

    @Test
    void detect_rubyRails_shouldDetectBackend() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("gemfile"),
                Map.of("gemfile", "source 'https://rubygems.org'\ngem 'rails', '~> 7.0'")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertEquals("BACKEND", result.getProjectType());
        assertTrue(result.getTechStack().contains("Ruby"));
        assertTrue(result.getTechStack().contains("Rails"));
    }

    // --- Frontend ---

    @Test
    void detect_reactViteTs_shouldDetectFrontend() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("package.json", "vite.config.ts", "tsconfig.json"),
                Map.of("package.json", "{\"dependencies\":{\"react\":\"18.0\"}}")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertEquals("FRONTEND", result.getProjectType());
        assertTrue(result.getTechStack().contains("React"));
        assertTrue(result.getTechStack().contains("TypeScript"));
        assertTrue(result.getTechStack().contains("Vite"));
    }

    @Test
    void detect_angular_shouldDetectFrontend() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("package.json"),
                Map.of("package.json", "{\"dependencies\":{\"@angular/core\":\"17.0\"}}")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertTrue(result.getTechStack().contains("Angular"));
    }

    // --- Docker / Infra ---

    @Test
    void detect_docker_shouldAddDocker() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("package.json", "dockerfile"),
                Map.of("package.json", "{}")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertTrue(result.getTechStack().contains("Docker"));
    }

    // --- Database ---

    @Test
    void detect_mysqlByRequirements_shouldDetectMySQL() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("requirements.txt"),
                Map.of("requirements.txt", "pymysql==1.1.0\nfastapi==0.115.0")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertTrue(result.getTechStack().contains("MySQL"));
    }

    @Test
    void detect_postgresByRequirements_shouldDetectPostgres() {
        ProjectScanner.ProjectScanContext ctx = buildContext(
                Set.of("requirements.txt"),
                Map.of("requirements.txt", "psycopg2-binary==2.9.0\nfastapi==0.115.0")
        );

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertTrue(result.getTechStack().contains("PostgreSQL"));
    }

    // --- Unknown ---

    @Test
    void detect_nothingDetected_shouldReturnUnknown() {
        ProjectScanner.ProjectScanContext ctx = buildContext(Set.of(), Map.of());

        TechStackDetector.TechStackDetectResult result = detector.detect(ctx);

        assertEquals("UNKNOWN", result.getProjectType());
        assertTrue(result.getTechStack().isEmpty());
    }

    private ProjectScanner.ProjectScanContext buildContext(Set<String> markers, Map<String, String> samples) {
        return new ProjectScanner.ProjectScanContext(
                "/tmp/test",
                10L, 5L,
                false, false,
                new ArrayList<>(markers),
                new LinkedHashSet<>(markers),
                List.of(),
                new HashMap<>(samples)
        );
    }
}

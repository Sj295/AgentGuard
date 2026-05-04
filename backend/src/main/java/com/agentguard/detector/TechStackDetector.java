package com.agentguard.detector;

import com.agentguard.scanner.ProjectScanner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Component
public class TechStackDetector {

    public TechStackDetectResult detect(ProjectScanner.ProjectScanContext context) {
        LinkedHashSet<String> techStack = new LinkedHashSet<>();

        // --- Java / JVM ---
        boolean hasPom = context.hasDetectedFile("pom.xml");
        boolean hasGradle = context.hasDetectedFile("build.gradle") || context.hasDetectedFile("build.gradle.kts");
        if (hasPom) {
            techStack.add("Java");
            techStack.add("Maven");
            if (contains(context.getSampleContent("pom.xml"), "spring-boot")) {
                techStack.add("Spring Boot");
            }
        }
        if (hasGradle) {
            techStack.add("Java");
            techStack.add("Gradle");
        }

        // --- Python ---
        boolean hasRequirements = context.hasDetectedFile("requirements.txt");
        boolean hasPyproject = context.hasDetectedFile("pyproject.toml");
        boolean hasSetupPy = context.hasDetectedFile("setup.py");
        boolean hasPipfile = context.hasDetectedFile("pipfile");
        boolean isPython = hasRequirements || hasPyproject || hasSetupPy || hasPipfile;
        if (isPython) {
            techStack.add("Python");
            String reqContent = context.getSampleContent("requirements.txt");
            String pyprojectContent = context.getSampleContent("pyproject.toml");
            String setupContent = context.getSampleContent("setup.py");
            String pipfileContent = context.getSampleContent("pipfile");
            String allPythonContent = (reqContent + " " + pyprojectContent + " " + setupContent + " " + pipfileContent).toLowerCase(Locale.ROOT);
            if (allPythonContent.contains("fastapi") || allPythonContent.contains("uvicorn")) {
                techStack.add("FastAPI");
            }
            if (allPythonContent.contains("django")) {
                techStack.add("Django");
            }
            if (allPythonContent.contains("flask")) {
                techStack.add("Flask");
            }
        }

        // --- Go ---
        boolean hasGoMod = context.hasDetectedFile("go.mod");
        if (hasGoMod) {
            techStack.add("Go");
            String goModContent = context.getSampleContent("go.mod").toLowerCase(Locale.ROOT);
            if (goModContent.contains("gin-gonic") || goModContent.contains("github.com/gin-gonic")) {
                techStack.add("Gin");
            }
            if (goModContent.contains("labstack/echo")) {
                techStack.add("Echo");
            }
            if (goModContent.contains("gofiber")) {
                techStack.add("Fiber");
            }
        }

        // --- Rust ---
        boolean hasCargo = context.hasDetectedFile("cargo.toml");
        if (hasCargo) {
            techStack.add("Rust");
            String cargoContent = context.getSampleContent("cargo.toml").toLowerCase(Locale.ROOT);
            if (cargoContent.contains("actix-web")) {
                techStack.add("Actix");
            }
            if (cargoContent.contains("axum")) {
                techStack.add("Axum");
            }
            if (cargoContent.contains("rocket")) {
                techStack.add("Rocket");
            }
        }

        // --- PHP ---
        boolean hasComposer = context.hasDetectedFile("composer.json");
        if (hasComposer) {
            techStack.add("PHP");
            String composerContent = context.getSampleContent("composer.json").toLowerCase(Locale.ROOT);
            if (composerContent.contains("laravel")) {
                techStack.add("Laravel");
            }
            if (composerContent.contains("symfony")) {
                techStack.add("Symfony");
            }
        }

        // --- Ruby ---
        boolean hasGemfile = context.hasDetectedFile("gemfile");
        if (hasGemfile) {
            techStack.add("Ruby");
            String gemfileContent = context.getSampleContent("gemfile").toLowerCase(Locale.ROOT);
            if (gemfileContent.contains("rails")) {
                techStack.add("Rails");
            }
            if (gemfileContent.contains("sinatra")) {
                techStack.add("Sinatra");
            }
        }

        // --- C / C++ ---
        boolean hasCmake = context.hasDetectedFile("cmakelists.txt");
        boolean hasMakefile = context.hasDetectedFile("makefile");
        if (hasCmake || hasMakefile) {
            techStack.add("C/C++");
            if (hasCmake) {
                techStack.add("CMake");
            }
        }

        // --- Swift ---
        boolean hasSwiftPm = context.hasDetectedFile("package.swift");
        if (hasSwiftPm) {
            techStack.add("Swift");
        }

        // --- Node.js / Frontend ---
        boolean hasPackageJson = context.hasDetectedFile("package.json");
        boolean hasVite = context.hasDetectedFile("vite.config.ts") || context.hasDetectedFile("vite.config.js");
        boolean hasTsConfig = context.hasDetectedFile("tsconfig.json");
        if (hasPackageJson) {
            techStack.add("Node.js");
            String packageJson = context.getSampleContent("package.json").toLowerCase(Locale.ROOT);
            if (packageJson.contains("vue")) {
                techStack.add("Vue");
            }
            if (packageJson.contains("react")) {
                techStack.add("React");
            }
            if (packageJson.contains("angular") || packageJson.contains("@angular/core")) {
                techStack.add("Angular");
            }
            if (packageJson.contains("svelte")) {
                techStack.add("Svelte");
            }
            if (packageJson.contains("\"next\"") || packageJson.contains("\"next\":") || packageJson.contains("@next/")) {
                techStack.add("Next.js");
            }
            if (packageJson.contains("nuxt")) {
                techStack.add("Nuxt");
            }
            if (packageJson.contains("express")) {
                techStack.add("Express");
            }
            if (packageJson.contains("nestjs") || packageJson.contains("@nestjs/core")) {
                techStack.add("NestJS");
            }
        }
        if (hasVite) {
            techStack.add("Vite");
        }
        if (hasTsConfig) {
            techStack.add("TypeScript");
        }

        // --- Docker ---
        boolean hasDocker = context.hasDetectedFile("dockerfile") || context.hasDetectedFile("docker-compose.yml")
                || context.hasDetectedFile("docker-compose.yaml");
        if (hasDocker) {
            techStack.add("Docker");
        }

        // --- Database detection ---
        boolean hasMySqlByPom = contains(context.getSampleContent("pom.xml"), "mysql");
        boolean hasMySqlByApplication = contains(context.getSampleContent("application.yml"), "datasource")
                || contains(context.getSampleContent("application.yml"), "mysql")
                || contains(context.getSampleContent("application.yaml"), "datasource")
                || contains(context.getSampleContent("application.yaml"), "mysql");
        String allContent = (context.getSampleContent("requirements.txt") + " " + context.getSampleContent("pyproject.toml")
                + " " + context.getSampleContent("go.mod") + " " + context.getSampleContent("cargo.toml")).toLowerCase(Locale.ROOT);
        boolean hasMySqlByOther = allContent.contains("mysql") || allContent.contains("pymysql") || allContent.contains("sqlalchemy");
        boolean hasPostgres = allContent.contains("postgres") || allContent.contains("psycopg")
                || contains(context.getSampleContent("application.yml"), "postgresql")
                || contains(context.getSampleContent("application.yaml"), "postgresql");
        boolean hasRedis = allContent.contains("redis") || contains(context.getSampleContent("pom.xml"), "redis");
        boolean hasMongo = allContent.contains("mongo") || contains(context.getSampleContent("pom.xml"), "mongo");
        if (hasMySqlByPom || hasMySqlByApplication || hasMySqlByOther) {
            techStack.add("MySQL");
        }
        if (hasPostgres) {
            techStack.add("PostgreSQL");
        }
        if (hasRedis) {
            techStack.add("Redis");
        }
        if (hasMongo) {
            techStack.add("MongoDB");
        }

        // --- Project type classification ---
        boolean isBackend = hasPom || hasGradle || isPython || hasGoMod || hasCargo || hasComposer || hasGemfile
                || hasCmake || hasMakefile || hasSwiftPm;
        boolean isFrontend = hasPackageJson || hasVite;
        String projectType;
        if (isBackend && isFrontend) {
            projectType = "FULL_STACK";
        } else if (isBackend) {
            projectType = "BACKEND";
        } else if (isFrontend) {
            projectType = "FRONTEND";
        } else {
            projectType = "UNKNOWN";
        }

        return new TechStackDetectResult(projectType, new ArrayList<>(techStack));
    }

    private boolean contains(String content, String fragment) {
        return content != null && content.toLowerCase(Locale.ROOT).contains(fragment.toLowerCase(Locale.ROOT));
    }

    public static class TechStackDetectResult {

        private final String projectType;
        private final List<String> techStack;

        public TechStackDetectResult(String projectType, List<String> techStack) {
            this.projectType = projectType;
            this.techStack = techStack;
        }

        public String getProjectType() {
            return projectType;
        }

        public List<String> getTechStack() {
            return techStack;
        }
    }
}

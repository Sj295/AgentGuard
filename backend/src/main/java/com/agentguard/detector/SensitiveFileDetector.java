package com.agentguard.detector;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class SensitiveFileDetector {

    private static final Set<String> EXACT_SENSITIVE_FILE_NAMES = Set.of(
            ".env",
            ".env.local",
            ".env.production",
            "id_rsa",
            "id_rsa.pub",
            "application-prod.yml",
            "application-prod.yaml",
            "docker-compose.prod.yml"
    );

    private static final Set<String> SENSITIVE_SUFFIXES = Set.of(".pem", ".key", ".p12", ".jks");
    private static final Set<String> SENSITIVE_PATH_KEYWORDS = Set.of("secret", "secrets", "credential", "credentials", "token");

    public List<String> detectSensitiveFiles(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> sensitiveFiles = new LinkedHashSet<>();
        for (String filePath : filePaths) {
            if (filePath == null || filePath.isBlank()) {
                continue;
            }
            String normalizedPath = filePath.replace('\\', '/');
            String pathLower = normalizedPath.toLowerCase(Locale.ROOT);
            String fileNameLower = extractFileName(pathLower);
            if (EXACT_SENSITIVE_FILE_NAMES.contains(fileNameLower)
                    || hasSensitiveSuffix(fileNameLower)
                    || containsSensitiveKeyword(pathLower)) {
                sensitiveFiles.add(normalizedPath);
            }
        }
        return new ArrayList<>(sensitiveFiles);
    }

    private String extractFileName(String filePathLower) {
        int separatorIndex = filePathLower.lastIndexOf('/');
        return separatorIndex >= 0 ? filePathLower.substring(separatorIndex + 1) : filePathLower;
    }

    private boolean hasSensitiveSuffix(String fileNameLower) {
        for (String suffix : SENSITIVE_SUFFIXES) {
            if (fileNameLower.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsSensitiveKeyword(String pathLower) {
        for (String keyword : SENSITIVE_PATH_KEYWORDS) {
            if (pathLower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}

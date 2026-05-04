package com.agentguard.detector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveFileDetectorTest {

    private SensitiveFileDetector detector;

    @BeforeEach
    void setUp() {
        detector = new SensitiveFileDetector();
    }

    @Test
    void detect_envFile_shouldBeSensitive() {
        List<String> result = detector.detectSensitiveFiles(List.of("backend/.env", "src/main.js"));

        assertEquals(1, result.size());
        assertEquals("backend/.env", result.get(0));
    }

    @Test
    void detect_envLocal_shouldBeSensitive() {
        List<String> result = detector.detectSensitiveFiles(List.of(".env.local"));

        assertEquals(1, result.size());
    }

    @Test
    void detect_idRsa_shouldBeSensitive() {
        List<String> result = detector.detectSensitiveFiles(List.of("id_rsa", "id_rsa.pub"));

        assertEquals(2, result.size());
    }

    @Test
    void detect_pemFile_shouldBeSensitive() {
        List<String> result = detector.detectSensitiveFiles(List.of("certs/server.pem", "ssl/key.p12"));

        assertEquals(2, result.size());
    }

    @Test
    void detect_keyFile_shouldBeSensitive() {
        List<String> result = detector.detectSensitiveFiles(List.of("secrets/private.key"));

        assertEquals(1, result.size());
    }

    @Test
    void detect_tokenInPath_shouldBeSensitive() {
        List<String> result = detector.detectSensitiveFiles(List.of("src/auth/token.json", "lib/tokenService.js"));

        assertEquals(2, result.size());
    }

    @Test
    void detect_credentialInPath_shouldBeSensitive() {
        List<String> result = detector.detectSensitiveFiles(List.of("config/credentials/aws.json"));

        assertEquals(1, result.size());
    }

    @Test
    void detect_applicationProdYml_shouldBeSensitive() {
        List<String> result = detector.detectSensitiveFiles(List.of("src/main/resources/application-prod.yml"));

        assertEquals(1, result.size());
    }

    @Test
    void detect_normalFiles_shouldNotBeSensitive() {
        List<String> result = detector.detectSensitiveFiles(List.of(
                "src/index.js", "README.md", "package.json", "pom.xml"
        ));

        assertTrue(result.isEmpty());
    }

    @Test
    void detect_mixedFiles_shouldOnlyReturnSensitive() {
        List<String> result = detector.detectSensitiveFiles(List.of(
                "src/index.js", "backend/.env", "README.md", "certs/server.pem"
        ));

        assertEquals(2, result.size());
        assertTrue(result.contains("backend/.env"));
        assertTrue(result.contains("certs/server.pem"));
    }

    @Test
    void detect_nullInput_shouldReturnEmpty() {
        List<String> result = detector.detectSensitiveFiles(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void detect_emptyInput_shouldReturnEmpty() {
        List<String> result = detector.detectSensitiveFiles(List.of());

        assertTrue(result.isEmpty());
    }
}

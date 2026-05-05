package com.agentguard.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:agentguard_ai_empty_key_it;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/integration-schema.sql",
        "agentguard.ai.enabled=true",
        "agentguard.ai.api-key=",
        "agentguard.ai.mock-on-empty-key=true",
        "spring.ai.openai.api-key=",
        "mybatis-plus.configuration.map-underscore-to-camel-case=true",
        "mybatis-plus.global-config.db-config.logic-delete-field=deleted",
        "mybatis-plus.global-config.db-config.logic-delete-value=1",
        "mybatis-plus.global-config.db-config.logic-not-delete-value=0"
})
class AiAnalysisEmptyApiKeyIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TempDir
    private Path projectDir;

    @Test
    void shouldFallbackToMockWhenApiKeyIsEmpty() throws IOException {
        Map<String, Object> status = get("/api/ai/status");
        assertThat(status.get("executionMode")).isEqualTo("MOCK_EMPTY_KEY");
        assertThat(status.get("willCallRemoteModel")).isEqualTo(false);
        assertThat(status.get("hasApiKey")).isEqualTo(false);
        assertThat((String) status.get("statusText")).contains("未配置 API Key");

        prepareProject(projectDir);
        Map<String, Object> scan = post("/api/projects/scan", Map.of(
                "projectName", "AgentGuard AI Empty Key IT",
                "projectPath", projectDir.toString()
        ));
        long projectId = longValue(scan, "projectId");

        Map<String, Object> summary = post("/api/ai/report/summary", Map.of(
                "projectId", projectId,
                "markdown", "# Empty Key Test\n\n- No API key configured"
        ));

        assertThat(summary.get("mocked")).isEqualTo(true);
        assertThat((String) summary.get("confidenceNote")).contains("仅供参考");

        Integer recordCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_analysis_record WHERE project_id = ? AND analysis_type = 'REPORT_SUMMARY'",
                Integer.class,
                projectId
        );
        assertThat(recordCount).isEqualTo(1);
    }

    private void prepareProject(Path root) throws IOException {
        Files.writeString(root.resolve("package.json"), """
                {"scripts":{"build":"vite build"}}
                """);
        Files.writeString(root.resolve("README.md"), "# AI Empty Key Integration Test\n");
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        ResponseEntity<Map> response = restTemplate.postForEntity(url(path), body, Map.class);
        return data(response);
    }

    private Map<String, Object> get(String path) {
        ResponseEntity<Map> response = restTemplate.getForEntity(url(path), Map.class);
        return data(response);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("code")).isEqualTo(0);
        assertThat(body.get("data")).isNotNull();
        return (Map<String, Object>) body.get("data");
    }

    private long longValue(Map<String, Object> map, String key) {
        return ((Number) map.get(key)).longValue();
    }
}

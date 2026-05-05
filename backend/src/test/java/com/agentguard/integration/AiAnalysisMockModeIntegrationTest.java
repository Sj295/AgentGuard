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
        "spring.datasource.url=jdbc:h2:mem:agentguard_ai_mock_it;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/integration-schema.sql",
        "agentguard.ai.enabled=false",
        "mybatis-plus.configuration.map-underscore-to-camel-case=true",
        "mybatis-plus.global-config.db-config.logic-delete-field=deleted",
        "mybatis-plus.global-config.db-config.logic-delete-value=1",
        "mybatis-plus.global-config.db-config.logic-not-delete-value=0"
})
class AiAnalysisMockModeIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TempDir
    private Path projectDir;

    @Test
    void shouldUseMockForAllAiEndpointsAndPersistRecords() throws IOException {
        prepareProject(projectDir);
        Map<String, Object> scan = post("/api/projects/scan", Map.of(
                "projectName", "AgentGuard AI Mock IT",
                "projectPath", projectDir.toString()
        ));
        long projectId = longValue(scan, "projectId");

        long gitReportId = insertRiskReport(
                projectId,
                "GIT_DIFF_AUDIT",
                "HIGH",
                "Git diff risk summary",
                "[\"变更了配置文件\"]",
                "[\"建议运行回归测试\"]",
                "{\"addedFiles\":[\"src/main/resources/application.yml\"],\"modifiedFiles\":[\"pom.xml\"],\"deletedFiles\":[]}"
        );
        long explainReportId = insertRiskReport(
                projectId,
                "COMMAND_AUDIT",
                "CRITICAL",
                "Dangerous commands detected",
                "[\"检测到 rm -rf 命令\"]",
                "[\"移除危险命令\"]",
                "{\"commands\":[\"rm -rf /\"]}"
        );

        Map<String, Object> gitDiff = post("/api/ai/git-diff/analyze", Map.of(
                "projectId", projectId,
                "gitAuditReportId", gitReportId
        ));
        Map<String, Object> riskExplain = post("/api/ai/risk/explain", Map.of(
                "projectId", projectId,
                "reportId", explainReportId
        ));
        Map<String, Object> reportSummary = post("/api/ai/report/summary", Map.of(
                "projectId", projectId,
                "markdown", "# AgentGuard Security Report\n\n- Risk: HIGH"
        ));

        assertThat(gitDiff.get("mocked")).isEqualTo(true);
        assertThat(riskExplain.get("mocked")).isEqualTo(true);
        assertThat(reportSummary.get("mocked")).isEqualTo(true);
        assertThat((String) gitDiff.get("confidenceNote")).contains("仅供参考");
        assertThat((String) riskExplain.get("confidenceNote")).contains("仅供参考");
        assertThat((String) reportSummary.get("confidenceNote")).contains("仅供参考");

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_analysis_record WHERE project_id = ?",
                Integer.class,
                projectId
        );
        Integer mockedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_analysis_record WHERE project_id = ? AND mocked = TRUE",
                Integer.class,
                projectId
        );
        assertThat(total).isEqualTo(3);
        assertThat(mockedCount).isEqualTo(3);
    }

    private long insertRiskReport(long projectId,
                                  String reportType,
                                  String riskLevel,
                                  String summary,
                                  String riskItems,
                                  String suggestions,
                                  String payloadJson) {
        jdbcTemplate.update("""
                INSERT INTO risk_report (
                    project_id, report_type, risk_level, risk_score, summary,
                    risk_items, suggestions, payload_json, created_time, deleted
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 0)
                """,
                projectId, reportType, riskLevel, 80, summary, riskItems, suggestions, payloadJson);

        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM risk_report WHERE project_id = ? AND report_type = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                projectId,
                reportType
        );
        assertThat(id).isNotNull();
        return id;
    }

    private void prepareProject(Path root) throws IOException {
        Files.writeString(root.resolve("package.json"), """
                {"scripts":{"build":"vite build"}}
                """);
        Files.writeString(root.resolve("README.md"), "# AI Mock Mode Integration Test\n");
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        ResponseEntity<Map> response = restTemplate.postForEntity(url(path), body, Map.class);
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

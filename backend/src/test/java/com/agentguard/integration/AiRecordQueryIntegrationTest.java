package com.agentguard.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:agentguard_ai_record_query_it;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
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
class AiRecordQueryIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TempDir
    private Path projectDir;

    private long projectId;

    @BeforeEach
    void setUp() throws IOException {
        prepareProject(projectDir);
        Map<String, Object> scan = post("/api/projects/scan", Map.of(
                "projectName", "AgentGuard Record Query IT",
                "projectPath", projectDir.toString()
        ));
        projectId = longValue(scan, "projectId");

        // Generate multiple AI records by calling all 3 endpoints
        long gitReportId = insertRiskReport(projectId, "GIT_DIFF_AUDIT", "HIGH",
                "Git diff summary", "[\"变更了配置\"]", "[\"运行测试\"]",
                "{\"addedFiles\":[\"src/Main.java\"]}");
        long commandReportId = insertRiskReport(projectId, "COMMAND_AUDIT", "CRITICAL",
                "Command risk", "[\"rm -rf\"]", "[\"移除危险命令\"]",
                "{\"commands\":[\"rm -rf /\"]}");

        post("/api/ai/git-diff/analyze", Map.of("projectId", projectId, "gitAuditReportId", gitReportId));
        post("/api/ai/risk/explain", Map.of("projectId", projectId, "reportId", commandReportId));
        post("/api/ai/report/summary", Map.of("projectId", projectId, "markdown", "# Report\n- Risk: HIGH"));
    }

    @Test
    void pageProjectRecords_shouldReturnPaginatedResults() {
        Map<String, Object> page = get("/api/ai/records/project/" + projectId + "?current=1&size=10");

        assertThat(page.get("total")).isEqualTo(3);
        assertThat(page.get("current")).isEqualTo(1);
        assertThat(page.get("size")).isEqualTo(10);
        List<Map<String, Object>> records = records(page);
        assertThat(records).hasSize(3);
    }

    @Test
    void pageProjectRecords_shouldSupportPagination() {
        Map<String, Object> page = get("/api/ai/records/project/" + projectId + "?current=1&size=2");

        assertThat(page.get("total")).isEqualTo(3);
        assertThat(page.get("size")).isEqualTo(2);
        List<Map<String, Object>> records = records(page);
        assertThat(records).hasSize(2);
    }

    @Test
    void pageProjectRecords_shouldSupportSecondPage() {
        Map<String, Object> page = get("/api/ai/records/project/" + projectId + "?current=2&size=2");

        assertThat(page.get("total")).isEqualTo(3);
        List<Map<String, Object>> records = records(page);
        assertThat(records).hasSize(1);
    }

    @Test
    void pageProjectRecords_shouldFilterByAnalysisType() {
        Map<String, Object> page = get("/api/ai/records/project/" + projectId + "?current=1&size=10&analysisType=GIT_DIFF_ANALYSIS");

        assertThat(page.get("total")).isEqualTo(1);
        List<Map<String, Object>> records = records(page);
        assertThat(records).hasSize(1);
        assertThat(records.get(0).get("analysisType")).isEqualTo("GIT_DIFF_ANALYSIS");
    }

    @Test
    void pageProjectRecords_shouldFilterByRiskExplainType() {
        Map<String, Object> page = get("/api/ai/records/project/" + projectId + "?current=1&size=10&analysisType=RISK_EXPLAIN");

        assertThat(page.get("total")).isEqualTo(1);
        List<Map<String, Object>> records = records(page);
        assertThat(records.get(0).get("analysisType")).isEqualTo("RISK_EXPLAIN");
    }

    @Test
    void pageProjectRecords_shouldFilterByReportSummaryType() {
        Map<String, Object> page = get("/api/ai/records/project/" + projectId + "?current=1&size=10&analysisType=REPORT_SUMMARY");

        assertThat(page.get("total")).isEqualTo(1);
        List<Map<String, Object>> records = records(page);
        assertThat(records.get(0).get("analysisType")).isEqualTo("REPORT_SUMMARY");
    }

    @Test
    @SuppressWarnings("unchecked")
    void pageProjectRecords_shouldReturnErrorForNonExistentType() {
        // Non-existent analysis type returns error code in Result body
        ResponseEntity<Map> response = restTemplate.getForEntity(
                url("/api/ai/records/project/" + projectId + "?current=1&size=10&analysisType=NON_EXISTENT"), Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("code")).isEqualTo(400);
    }

    @Test
    void getRecordDetail_shouldReturnFullRecord() {
        // Get a record ID first
        Map<String, Object> page = get("/api/ai/records/project/" + projectId + "?current=1&size=1");
        List<Map<String, Object>> records = records(page);
        long recordId = longValue(records.get(0), "id");

        Map<String, Object> detail = get("/api/ai/records/" + recordId);

        assertThat(detail.get("id")).isEqualTo((int) recordId);
        assertThat(detail.get("projectId")).isEqualTo((int) projectId);
        assertThat(detail.get("analysisType")).isNotNull();
        assertThat(detail.get("provider")).isEqualTo("mock");
        assertThat(detail.get("model")).isEqualTo("mock-model");
        assertThat(detail.get("mocked")).isEqualTo(true);
        assertThat(detail.get("success")).isEqualTo(true);
        assertThat(detail.get("latencyMs")).isNotNull();
        assertThat(detail.get("inputSummary")).isNotNull();
        assertThat(detail.get("outputContent")).isNotNull();
        assertThat(detail.get("createdTime")).isNotNull();
    }

    @Test
    void listLatestProjectRecords_shouldReturnLatestN() {
        List<Map<String, Object>> records = getList("/api/ai/records/project/" + projectId + "/latest?limit=2");
        assertThat(records).hasSize(2);
    }

    @Test
    void listLatestProjectRecords_shouldReturnAllWhenLimitExceedsTotal() {
        List<Map<String, Object>> records = getList("/api/ai/records/project/" + projectId + "/latest?limit=100");
        assertThat(records).hasSize(3);
    }

    @Test
    void listLatestProjectRecords_shouldDefaultTo5() {
        List<Map<String, Object>> records = getList("/api/ai/records/project/" + projectId + "/latest");
        assertThat(records).hasSize(3); // only 3 records exist
    }

    @Test
    void recordDetail_shouldContainAllExpectedFields() {
        Map<String, Object> page = get("/api/ai/records/project/" + projectId + "?current=1&size=1&analysisType=GIT_DIFF_ANALYSIS");
        List<Map<String, Object>> records = records(page);
        long recordId = longValue(records.get(0), "id");

        Map<String, Object> detail = get("/api/ai/records/" + recordId);

        assertThat(detail).containsKey("id");
        assertThat(detail).containsKey("projectId");
        assertThat(detail).containsKey("analysisType");
        assertThat(detail).containsKey("sourceReportId");
        assertThat(detail).containsKey("provider");
        assertThat(detail).containsKey("model");
        assertThat(detail).containsKey("mocked");
        assertThat(detail).containsKey("success");
        assertThat(detail).containsKey("latencyMs");
        assertThat(detail).containsKey("inputSummary");
        assertThat(detail).containsKey("outputContent");
        assertThat(detail).containsKey("errorMessage");
        assertThat(detail).containsKey("createdTime");
    }

    @Test
    void recordDetail_gitDiffRecordShouldHaveSourceReportId() {
        Map<String, Object> page = get("/api/ai/records/project/" + projectId + "?current=1&size=1&analysisType=GIT_DIFF_ANALYSIS");
        List<Map<String, Object>> records = records(page);
        long recordId = longValue(records.get(0), "id");

        Map<String, Object> detail = get("/api/ai/records/" + recordId);

        assertThat(detail.get("sourceReportId")).isNotNull();
        assertThat(detail.get("analysisType")).isEqualTo("GIT_DIFF_ANALYSIS");
    }

    @Test
    void recordDetail_reportSummaryRecordShouldHaveNullSourceReportId() {
        Map<String, Object> page = get("/api/ai/records/project/" + projectId + "?current=1&size=1&analysisType=REPORT_SUMMARY");
        List<Map<String, Object>> records = records(page);
        long recordId = longValue(records.get(0), "id");

        Map<String, Object> detail = get("/api/ai/records/" + recordId);

        assertThat(detail.get("sourceReportId")).isNull();
        assertThat(detail.get("analysisType")).isEqualTo("REPORT_SUMMARY");
    }

    // ========== Helpers ==========

    private long insertRiskReport(long projectId, String reportType, String riskLevel,
                                  String summary, String riskItems, String suggestions, String payloadJson) {
        jdbcTemplate.update("""
                INSERT INTO risk_report (
                    project_id, report_type, risk_level, risk_score, summary,
                    risk_items, suggestions, payload_json, created_time, deleted
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, 0)
                """, projectId, reportType, riskLevel, 80, summary, riskItems, suggestions, payloadJson);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM risk_report WHERE project_id = ? AND report_type = ? ORDER BY id DESC LIMIT 1",
                Long.class, projectId, reportType);
    }

    private void prepareProject(Path root) throws IOException {
        Files.writeString(root.resolve("package.json"), """
                {"scripts":{"build":"vite build"}}
                """);
        Files.writeString(root.resolve("README.md"), "# Record Query IT\n");
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        ResponseEntity<Map> response = restTemplate.postForEntity(url(path), body, Map.class);
        return data(response);
    }

    private Map<String, Object> get(String path) {
        ResponseEntity<Map> response = restTemplate.getForEntity(url(path), Map.class);
        return data(response);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(String path) {
        ResponseEntity<Map> response = restTemplate.getForEntity(url(path), Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("code")).isEqualTo(0);
        assertThat(body.get("data")).isNotNull();
        return (List<Map<String, Object>>) body.get("data");
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> records(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("records");
    }

    private long longValue(Map<String, Object> map, String key) {
        return ((Number) map.get(key)).longValue();
    }
}

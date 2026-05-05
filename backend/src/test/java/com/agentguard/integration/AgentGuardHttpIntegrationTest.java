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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:agentguard_it;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/integration-schema.sql",
        "mybatis-plus.configuration.map-underscore-to-camel-case=true",
        "mybatis-plus.global-config.db-config.logic-delete-field=deleted",
        "mybatis-plus.global-config.db-config.logic-delete-value=1",
        "mybatis-plus.global-config.db-config.logic-not-delete-value=0"
})
class AgentGuardHttpIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @TempDir
    private Path projectDir;

    @Test
    void shouldRunFullHttpFlowWithTemporaryDatabase() throws IOException {
        prepareProject(projectDir);

        Map<String, Object> scan = post("/api/projects/scan", Map.of(
                "projectName", "AgentGuard IT",
                "projectPath", projectDir.toString()
        ));
        long projectId = longValue(scan, "projectId");
        long responseTaskId = longValue(scan, "taskId");
        assertThat(responseTaskId).isGreaterThan(0);

        Map<String, Object> taskPage = get("/api/scan-tasks/project/" + projectId + "?current=1&size=5");
        List<Map<String, Object>> taskRecords = records(taskPage);
        assertThat(taskRecords).isNotEmpty();
        long taskId = longValue(taskRecords.get(0), "id");

        Map<String, Object> task = get("/api/scan-tasks/" + taskId);
        assertThat(longValue(task, "projectId")).isEqualTo(projectId);
        assertThat(task.get("status")).isEqualTo("SUCCESS");
        assertThat(intValue(task, "progress")).isEqualTo(100);

        Map<String, Object> scanResult = get("/api/scan-tasks/" + taskId + "/result");
        assertThat(longValue(scanResult, "projectId")).isEqualTo(projectId);
        assertThat(longValue(scanResult, "taskId")).isEqualTo(taskId);
        assertThat(intValue(scanResult, "fileCount")).isGreaterThanOrEqualTo(3);
        assertThat((String) scanResult.get("sensitiveFiles")).contains(".env");

        Map<String, Object> permission = post("/api/risk/permission-assess", Map.of(
                "projectId", projectId,
                "agentType", "CODEX",
                "taskType", "NEW_FEATURE",
                "sandboxMode", "WORKSPACE_WRITE",
                "approvalPolicy", "AUTO_APPROVE",
                "networkAccess", false,
                "allowDelete", false
        ));
        long permissionReportId = longValue(permission, "reportId");
        int permissionScore = intValue(permission, "score");

        Map<String, Object> permissionDetail = get("/api/risk/reports/" + permissionReportId);
        assertThat(permissionDetail.get("reportType")).isEqualTo("PERMISSION_ASSESS");
        assertThat(intValue(permissionDetail, "riskScore")).isEqualTo(permissionScore);
        assertThat((String) permissionDetail.get("summary")).contains("风险分数");
        assertThat((String) permissionDetail.get("payloadJson")).contains("\"approvalPolicy\":\"AUTO_APPROVE\"");

        List<Map<String, Object>> permissionHistory = listData("/api/risk/reports/project/" + projectId);
        assertThat(permissionHistory).isNotEmpty();
        Map<String, Object> restoredPermission = permissionHistory.get(0);
        assertThat(restoredPermission.get("agentType")).isEqualTo("CODEX");
        assertThat(restoredPermission.get("taskType")).isEqualTo("NEW_FEATURE");
        assertThat(intValue(restoredPermission, "score")).isEqualTo(permissionScore);

        Map<String, Object> command = post("/api/commands/audit", Map.of(
                "projectId", projectId,
                "commands", List.of("rm -rf /")
        ));
        int commandScore = intValue(command, "score");
        assertThat(command.get("riskLevel")).isEqualTo("CRITICAL");
        assertThat(commandScore).isGreaterThanOrEqualTo(86);

        Map<String, Object> commandPage = get("/api/commands/reports/project/" + projectId + "?current=1&size=5");
        List<Map<String, Object>> commandRecords = records(commandPage);
        assertThat(commandRecords).hasSize(1);
        assertThat(intValue(commandRecords.get(0), "score")).isEqualTo(commandScore);
        assertThat(list(commandRecords.get(0), "safeAlternatives")).isNotEmpty();

        Map<String, Object> preflight = post("/api/preflight/check", Map.of(
                "projectId", projectId,
                "agentType", "CLAUDE",
                "taskType", "TEST_WRITING",
                "sandboxMode", "WORKSPACE_WRITE",
                "approvalPolicy", "AUTO_APPROVE",
                "networkAccess", false,
                "allowDelete", false,
                "plannedCommands", List.of("npm run build")
        ));
        int preflightScore = intValue(preflight, "score");
        List<Map<String, Object>> checkItems = list(preflight, "checkItems");
        assertThat(checkItems).isNotEmpty();

        Map<String, Object> preflightPage = get("/api/preflight/reports/project/" + projectId + "?current=1&size=5");
        List<Map<String, Object>> preflightRecords = records(preflightPage);
        assertThat(preflightRecords).hasSize(1);
        Map<String, Object> restoredPreflight = preflightRecords.get(0);
        assertThat(restoredPreflight.get("agentType")).isEqualTo("CLAUDE");
        assertThat(restoredPreflight.get("taskType")).isEqualTo("TEST_WRITING");
        assertThat(intValue(restoredPreflight, "score")).isEqualTo(preflightScore);
        assertThat(list(restoredPreflight, "checkItems")).hasSameSizeAs(checkItems);

        Map<String, Object> markdownReport = post("/api/reports/markdown/generate", Map.of(
                "projectId", projectId,
                "includeScanResult", true,
                "includeAgentRules", true,
                "includeRiskReports", true,
                "includeGitAudit", true,
                "includePreflight", true
        ));
        assertThat(markdownReport.get("projectId")).isEqualTo((int) projectId);
        assertThat((String) markdownReport.get("markdown")).contains("Risk Score");

        Map<String, Object> timeline = get("/api/timeline/project/" + projectId + "?current=1&size=10");
        assertThat(longValue(timeline, "total")).isGreaterThanOrEqualTo(4);
        assertThat(records(timeline))
                .extracting(record -> record.get("sourceType"))
                .contains("PROJECT_SCAN", "PERMISSION_ASSESS", "COMMAND_AUDIT", "PREFLIGHT_CHECK");

        Integer persistedReports = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM risk_report WHERE project_id = ? AND payload_json IS NOT NULL",
                Integer.class,
                projectId
        );
        assertThat(persistedReports).isEqualTo(3);
    }

    private void prepareProject(Path root) throws IOException {
        Files.writeString(root.resolve("package.json"), """
                {"scripts":{"build":"vite build","test":"vitest run"},"dependencies":{"vue":"latest"}}
                """);
        Files.writeString(root.resolve(".env"), "API_TOKEN=secret-token\n");
        Files.writeString(root.resolve("README.md"), "# Temporary AgentGuard Integration Project\n");
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> listData(String path) {
        ResponseEntity<Map> response = restTemplate.getForEntity(url(path), Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("code")).isEqualTo(0);
        return (List<Map<String, Object>>) body.get("data");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> records(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("records");
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> list(Map<String, Object> map, String key) {
        return (List<T>) map.get(key);
    }

    private long longValue(Map<String, Object> map, String key) {
        return ((Number) map.get(key)).longValue();
    }

    private int intValue(Map<String, Object> map, String key) {
        return ((Number) map.get(key)).intValue();
    }
}

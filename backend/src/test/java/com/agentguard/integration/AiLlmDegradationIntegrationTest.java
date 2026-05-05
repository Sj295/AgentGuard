package com.agentguard.integration;

import com.agentguard.ai.client.LlmClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:agentguard_ai_degradation_it;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/integration-schema.sql",
        "agentguard.ai.enabled=true",
        "agentguard.ai.api-key=test-key",
        "agentguard.ai.provider=test-provider",
        "agentguard.ai.model=test-model",
        "agentguard.ai.mock-on-empty-key=true",
        "spring.ai.openai.api-key=test-key",
        "mybatis-plus.configuration.map-underscore-to-camel-case=true",
        "mybatis-plus.global-config.db-config.logic-delete-field=deleted",
        "mybatis-plus.global-config.db-config.logic-delete-value=1",
        "mybatis-plus.global-config.db-config.logic-not-delete-value=0"
})
class AiLlmDegradationIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private LlmClient llmClient;

    @TempDir
    private Path projectDir;

    private long projectId;

    @BeforeEach
    void setUp() throws IOException {
        prepareProject(projectDir);
        Map<String, Object> scan = post("/api/projects/scan", Map.of(
                "projectName", "AgentGuard Degradation IT",
                "projectPath", projectDir.toString()
        ));
        projectId = longValue(scan, "projectId");
    }

    @Test
    void analyzeGitDiff_shouldFallbackToMockWhenLlmThrows() throws IOException {
        long reportId = insertRiskReport(projectId, "GIT_DIFF_AUDIT", "HIGH",
                "Git diff summary", "[\"变更了配置\"]", "[\"运行测试\"]",
                "{\"addedFiles\":[\"src/Main.java\"]}");
        when(llmClient.chat(anyString(), anyString())).thenThrow(new RuntimeException("Connection refused"));

        Map<String, Object> result = post("/api/ai/git-diff/analyze", Map.of(
                "projectId", projectId,
                "gitAuditReportId", reportId
        ));

        assertThat(result.get("mocked")).isEqualTo(true);
        assertThat((String) result.get("confidenceNote")).contains("仅供参考");
        assertThat(result.get("summary")).isNotNull();

        // Verify degraded record is persisted
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_analysis_record WHERE project_id = ? AND analysis_type = 'GIT_DIFF_ANALYSIS'",
                Integer.class, projectId);
        assertThat(count).isEqualTo(1);

        // Verify the record has error info
        String errorMessage = jdbcTemplate.queryForObject(
                "SELECT error_message FROM ai_analysis_record WHERE project_id = ? AND analysis_type = 'GIT_DIFF_ANALYSIS'",
                String.class, projectId);
        assertThat(errorMessage).contains("Connection refused");

        // Verify mocked=true and success=false in DB
        Boolean mocked = jdbcTemplate.queryForObject(
                "SELECT mocked FROM ai_analysis_record WHERE project_id = ? AND analysis_type = 'GIT_DIFF_ANALYSIS'",
                Boolean.class, projectId);
        assertThat(mocked).isTrue();

        Boolean success = jdbcTemplate.queryForObject(
                "SELECT success FROM ai_analysis_record WHERE project_id = ? AND analysis_type = 'GIT_DIFF_ANALYSIS'",
                Boolean.class, projectId);
        assertThat(success).isFalse();

        // Verify fallback provider
        String provider = jdbcTemplate.queryForObject(
                "SELECT provider FROM ai_analysis_record WHERE project_id = ? AND analysis_type = 'GIT_DIFF_ANALYSIS'",
                String.class, projectId);
        assertThat(provider).isEqualTo("fallback-mock");
    }

    @Test
    void explainRisk_shouldFallbackToMockWhenLlmThrows() {
        long reportId = insertRiskReport(projectId, "COMMAND_AUDIT", "CRITICAL",
                "Command risk", "[\"rm -rf\"]", "[\"移除危险命令\"]",
                "{\"commands\":[\"rm -rf /\"]}");
        when(llmClient.chat(anyString(), anyString())).thenThrow(new IllegalStateException("Failed to call Spring AI provider"));

        Map<String, Object> result = post("/api/ai/risk/explain", Map.of(
                "projectId", projectId,
                "reportId", reportId
        ));

        assertThat(result.get("mocked")).isEqualTo(true);
        assertThat((String) result.get("confidenceNote")).contains("仅供参考");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_analysis_record WHERE project_id = ? AND analysis_type = 'RISK_EXPLAIN'",
                Integer.class, projectId);
        assertThat(count).isEqualTo(1);

        Boolean success = jdbcTemplate.queryForObject(
                "SELECT success FROM ai_analysis_record WHERE project_id = ? AND analysis_type = 'RISK_EXPLAIN'",
                Boolean.class, projectId);
        assertThat(success).isFalse();
    }

    @Test
    void summarizeReport_shouldFallbackToMockWhenLlmThrows() {
        when(llmClient.chat(anyString(), anyString())).thenThrow(new RuntimeException("API timeout"));

        Map<String, Object> result = post("/api/ai/report/summary", Map.of(
                "projectId", projectId,
                "markdown", "# Report\n- Risk: HIGH"
        ));

        assertThat(result.get("mocked")).isEqualTo(true);
        assertThat((String) result.get("confidenceNote")).contains("仅供参考");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_analysis_record WHERE project_id = ? AND analysis_type = 'REPORT_SUMMARY'",
                Integer.class, projectId);
        assertThat(count).isEqualTo(1);

        String errorMessage = jdbcTemplate.queryForObject(
                "SELECT error_message FROM ai_analysis_record WHERE project_id = ? AND analysis_type = 'REPORT_SUMMARY'",
                String.class, projectId);
        assertThat(errorMessage).contains("API timeout");
    }

    @Test
    void analyzeGitDiff_shouldReturnRealResultWhenLlmSucceeds() {
        Map<String, Object> status = get("/api/ai/status");
        assertThat(status.get("executionMode")).isEqualTo("REAL_MODEL");
        assertThat(status.get("willCallRemoteModel")).isEqualTo(true);
        assertThat(status.get("provider")).isEqualTo("test-provider");
        assertThat(status.get("model")).isEqualTo("test-model");

        long reportId = insertRiskReport(projectId, "GIT_DIFF_AUDIT", "HIGH",
                "Git diff summary", "[\"变更了配置\"]", "[\"运行测试\"]",
                "{\"addedFiles\":[\"src/Main.java\"]}");
        String validJson = """
                {"summary":"变更影响分析完成","impactAreas":["配置模块"],"testSuggestions":["运行单元测试"],"rollbackSuggestions":["git restore"]}
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(validJson);

        Map<String, Object> result = post("/api/ai/git-diff/analyze", Map.of(
                "projectId", projectId,
                "gitAuditReportId", reportId
        ));

        assertThat(result.get("mocked")).isEqualTo(false);
        assertThat(result.get("summary")).isEqualTo("变更影响分析完成");
        assertThat((String) result.get("confidenceNote")).contains("仅供参考");

        // Verify success record
        Boolean mocked = jdbcTemplate.queryForObject(
                "SELECT mocked FROM ai_analysis_record WHERE project_id = ? AND analysis_type = 'GIT_DIFF_ANALYSIS'",
                Boolean.class, projectId);
        assertThat(mocked).isFalse();

        Boolean success = jdbcTemplate.queryForObject(
                "SELECT success FROM ai_analysis_record WHERE project_id = ? AND analysis_type = 'GIT_DIFF_ANALYSIS'",
                Boolean.class, projectId);
        assertThat(success).isTrue();

        String provider = jdbcTemplate.queryForObject(
                "SELECT provider FROM ai_analysis_record WHERE project_id = ? AND analysis_type = 'GIT_DIFF_ANALYSIS'",
                String.class, projectId);
        assertThat(provider).isEqualTo("test-provider");
    }

    @Test
    void explainRisk_shouldReturnRealResultWhenLlmSucceeds() {
        long reportId = insertRiskReport(projectId, "COMMAND_AUDIT", "CRITICAL",
                "Command risk", "[\"rm -rf\"]", "[\"移除危险命令\"]",
                "{\"commands\":[\"rm -rf /\"]}");
        String validJson = """
                {"riskSummary":"高危命令风险分析","whyItMatters":["数据丢失风险"],"fixPlan":["移除rm命令"],"safeNextSteps":["检查工作区"]}
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(validJson);

        Map<String, Object> result = post("/api/ai/risk/explain", Map.of(
                "projectId", projectId,
                "reportId", reportId
        ));

        assertThat(result.get("mocked")).isEqualTo(false);
        assertThat(result.get("riskSummary")).isEqualTo("高危命令风险分析");
    }

    @Test
    void summarizeReport_shouldReturnRealResultWhenLlmSucceeds() {
        String validJson = """
                {"executiveSummary":"安全报告摘要","keyFindings":["无高危漏洞"],"priorityActions":["持续监控"]}
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(validJson);

        Map<String, Object> result = post("/api/ai/report/summary", Map.of(
                "projectId", projectId,
                "markdown", "# Security Report\n- All clear"
        ));

        assertThat(result.get("mocked")).isEqualTo(false);
        assertThat(result.get("executiveSummary")).isEqualTo("安全报告摘要");
    }

    @Test
    void analyzeGitDiff_shouldFallbackWhenLlmReturnsMalformedJson() {
        long reportId = insertRiskReport(projectId, "GIT_DIFF_AUDIT", "HIGH",
                "Git diff summary", "[\"变更了配置\"]", "[\"运行测试\"]",
                "{\"addedFiles\":[\"src/Main.java\"]}");
        when(llmClient.chat(anyString(), anyString())).thenReturn("This is not JSON at all");

        Map<String, Object> result = post("/api/ai/git-diff/analyze", Map.of(
                "projectId", projectId,
                "gitAuditReportId", reportId
        ));

        // Should still return a valid result (text fallback, not mock)
        assertThat(result.get("summary")).isNotNull();
        assertThat((String) result.get("confidenceNote")).contains("仅供参考");
    }

    @Test
    void analyzeGitDiff_shouldFallbackWhenLlmReturnsJsonInCodeBlock() {
        long reportId = insertRiskReport(projectId, "GIT_DIFF_AUDIT", "HIGH",
                "Git diff summary", "[\"变更了配置\"]", "[\"运行测试\"]",
                "{\"addedFiles\":[\"src/Main.java\"]}");
        String codeBlockJson = """
                ```json
                {"summary":"代码块JSON","impactAreas":["模块A"],"testSuggestions":["测试"],"rollbackSuggestions":["回滚"]}
                ```
                """;
        when(llmClient.chat(anyString(), anyString())).thenReturn(codeBlockJson);

        Map<String, Object> result = post("/api/ai/git-diff/analyze", Map.of(
                "projectId", projectId,
                "gitAuditReportId", reportId
        ));

        assertThat(result.get("mocked")).isEqualTo(false);
        assertThat(result.get("summary")).isEqualTo("代码块JSON");
    }

    @Test
    void multipleFailures_shouldPersistAllDegradedRecords() {
        long gitReportId = insertRiskReport(projectId, "GIT_DIFF_AUDIT", "HIGH",
                "Git diff", "[\"r1\"]", "[\"s1\"]", "{\"addedFiles\":[\"a.java\"]}");
        long cmdReportId = insertRiskReport(projectId, "COMMAND_AUDIT", "CRITICAL",
                "Cmd risk", "[\"r2\"]", "[\"s2\"]", "{\"commands\":[\"rm\"]}");
        when(llmClient.chat(anyString(), anyString())).thenThrow(new RuntimeException("Service unavailable"));

        post("/api/ai/git-diff/analyze", Map.of("projectId", projectId, "gitAuditReportId", gitReportId));
        post("/api/ai/risk/explain", Map.of("projectId", projectId, "reportId", cmdReportId));
        post("/api/ai/report/summary", Map.of("projectId", projectId, "markdown", "# Report"));

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_analysis_record WHERE project_id = ?", Integer.class, projectId);
        assertThat(total).isEqualTo(3);

        Integer failedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_analysis_record WHERE project_id = ? AND success = FALSE",
                Integer.class, projectId);
        assertThat(failedCount).isEqualTo(3);

        Integer mockedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_analysis_record WHERE project_id = ? AND mocked = TRUE",
                Integer.class, projectId);
        assertThat(mockedCount).isEqualTo(3);
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
        Files.writeString(root.resolve("README.md"), "# Degradation IT\n");
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

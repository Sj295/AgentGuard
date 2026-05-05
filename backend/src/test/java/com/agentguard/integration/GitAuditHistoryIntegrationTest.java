package com.agentguard.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:agentguard_git_history_it;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
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
class GitAuditHistoryIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void gitAuditHistoryShouldRestoreStructuredPayloadFromPayloadJson() {
        long projectId = insertProject();
        long reportId = insertGitAuditReport(projectId);

        List<Map<String, Object>> reports = listData("/api/git-audit/reports/project/" + projectId);
        assertThat(reports).hasSize(1);
        Map<String, Object> latest = reports.get(0);
        assertThat(latest.get("reportId")).isEqualTo((int) reportId);
        assertThat(list(latest, "addedFiles")).containsExactly("src/NewFeature.java");
        assertThat(list(latest, "modifiedFiles")).containsExactly("pom.xml");
        assertThat(list(latest, "deletedFiles")).containsExactly("legacy/OldFile.java");
        assertThat(list(latest, "rollbackCommands")).containsExactly("git restore pom.xml");
        assertThat(list(latest, "suggestions")).containsExactly("建议执行回归测试");

        Map<String, Object> detail = data(restTemplate.getForEntity(url("/api/git-audit/reports/" + reportId), Map.class));
        assertThat(detail.get("id")).isEqualTo((int) reportId);
        assertThat(list(detail, "addedFiles")).containsExactly("src/NewFeature.java");
        assertThat(list(detail, "modifiedFiles")).containsExactly("pom.xml");
        assertThat(list(detail, "deletedFiles")).containsExactly("legacy/OldFile.java");
        assertThat(list(detail, "rollbackCommands")).containsExactly("git restore pom.xml");
        assertThat((String) detail.get("payloadJson")).contains("\"modifiedFiles\"");
    }

    private long insertProject() {
        jdbcTemplate.update("""
                INSERT INTO project_info (
                    project_name, project_path, project_type, tech_stack,
                    has_git, has_agents_md, created_time, updated_time, deleted
                ) VALUES (?, ?, ?, ?, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                """,
                "Git History IT",
                "D:/tmp/git-history-it",
                "BACKEND",
                "[\"Java\",\"Spring Boot\"]"
        );
        Long id = jdbcTemplate.queryForObject("SELECT id FROM project_info ORDER BY id DESC LIMIT 1", Long.class);
        assertThat(id).isNotNull();
        return id;
    }

    private long insertGitAuditReport(long projectId) {
        jdbcTemplate.update("""
                INSERT INTO risk_report (
                    project_id, report_type, risk_level, risk_score, summary,
                    risk_items, suggestions, payload_json, created_time, deleted
                ) VALUES (?, 'GIT_DIFF_AUDIT', 'HIGH', 88, ?, ?, ?, ?, CURRENT_TIMESTAMP, 0)
                """,
                projectId,
                "Git Diff 审计完成，变更文件 3 个，风险等级为 HIGH",
                "[\"修改了依赖配置文件\"]",
                "[\"legacy suggestion should not hide payload suggestions\"]",
                """
                        {
                          "changedFileCount": 3,
                          "score": 88,
                          "addedFiles": ["src/NewFeature.java"],
                          "modifiedFiles": ["pom.xml"],
                          "deletedFiles": ["legacy/OldFile.java"],
                          "suggestions": ["建议执行回归测试"],
                          "rollbackCommands": ["git restore pom.xml"]
                        }
                        """
        );
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM risk_report WHERE project_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                projectId
        );
        assertThat(id).isNotNull();
        return id;
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
    private List<String> list(Map<String, Object> map, String key) {
        return (List<String>) map.get(key);
    }
}

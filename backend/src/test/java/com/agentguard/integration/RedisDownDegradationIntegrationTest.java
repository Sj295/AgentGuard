package com.agentguard.integration;

import com.agentguard.ai.controller.AiAnalysisController;
import com.agentguard.ai.cache.AiAnalysisCacheService;
import com.agentguard.ai.cache.AiRateLimitService;
import com.agentguard.ai.dto.AiGitDiffAnalysisRequest;
import com.agentguard.ai.dto.AiReportSummaryRequest;
import com.agentguard.ai.dto.AiRiskExplainRequest;
import com.agentguard.ai.service.AiAnalysisService;
import com.agentguard.ai.vo.AiGitDiffAnalysisVO;
import com.agentguard.ai.vo.AiReportSummaryVO;
import com.agentguard.ai.vo.AiRiskExplainVO;
import com.agentguard.ai.vo.AiRuntimeStatusVO;
import com.agentguard.cache.RedisCacheService;
import com.agentguard.cache.RedisRateLimitService;
import com.agentguard.common.Result;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.RiskReport;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.RiskReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that AI endpoints work correctly when Redis is completely disabled.
 * This is the default deployment scenario — no Redis required.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:agentguard_redis_disabled_it;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/integration-schema.sql",
        "agentguard.ai.enabled=true",
        "agentguard.ai.api-key=",
        "agentguard.ai.mock-on-empty-key=true",
        "agentguard.redis.enabled=false",
        "agentguard.ai.cache.enabled=true",
        "agentguard.ai.rate-limit.enabled=true",
        "mybatis-plus.configuration.map-underscore-to-camel-case=true",
        "mybatis-plus.global-config.db-config.logic-delete-field=deleted",
        "mybatis-plus.global-config.db-config.logic-delete-value=1",
        "mybatis-plus.global-config.db-config.logic-not-delete-value=0"
})
class RedisDownDegradationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProjectInfoService projectInfoService;

    @Autowired
    private RiskReportService riskReportService;

    @Autowired(required = false)
    private RedisCacheService redisCacheService;

    @Autowired(required = false)
    private RedisRateLimitService redisRateLimitService;

    @Autowired(required = false)
    private AiAnalysisCacheService aiAnalysisCacheService;

    @Autowired(required = false)
    private AiRateLimitService aiRateLimitService;

    @Test
    void redisDependentBeans_shouldNotBeCreatedWhenRedisDisabled() {
        assertThat(redisCacheService).isNull();
        assertThat(redisRateLimitService).isNull();
        assertThat(aiAnalysisCacheService).isNull();
        assertThat(aiRateLimitService).isNull();
    }

    @Test
    void aiStatus_shouldReportRedisDisabled() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/ai/status", Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        Map data = (Map) response.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("redisEnabled")).isEqualTo(false);
        assertThat(data.get("redisAvailable")).isEqualTo(false);
        assertThat(data.get("cacheEnabled")).isEqualTo(false);
        assertThat(data.get("rateLimitEnabled")).isEqualTo(false);
    }

    @Test
    void analyzeGitDiff_shouldWorkWithoutRedis() {
        setupProjectAndReport("GIT_DIFF_AUDIT", 1L, 100L);

        AiGitDiffAnalysisRequest request = new AiGitDiffAnalysisRequest();
        request.setProjectId(1L);
        request.setGitAuditReportId(100L);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AiGitDiffAnalysisRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/ai/git-diff/analyze", entity, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        Map data = (Map) response.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("summary")).isNotNull();
        assertThat(data.get("confidenceNote")).isEqualTo(AiAnalysisService.CONFIDENCE_NOTE);
    }

    @Test
    void explainRisk_shouldWorkWithoutRedis() {
        setupProjectAndReport("COMMAND_AUDIT", 2L, 200L);

        AiRiskExplainRequest request = new AiRiskExplainRequest();
        request.setProjectId(2L);
        request.setReportId(200L);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AiRiskExplainRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/ai/risk/explain", entity, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        Map data = (Map) response.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("riskSummary")).isNotNull();
    }

    @Test
    void summarizeReport_shouldWorkWithoutRedis() {
        setupProject(3L);

        AiReportSummaryRequest request = new AiReportSummaryRequest();
        request.setProjectId(3L);
        request.setMarkdown("# Security Report\n- Risk: HIGH");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AiReportSummaryRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/ai/report/summary", entity, Map.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);

        Map data = (Map) response.getBody().get("data");
        assertThat(data).isNotNull();
        assertThat(data.get("executiveSummary")).isNotNull();
    }

    private void setupProjectAndReport(String reportType, Long projectId, Long reportId) {
        setupProject(projectId);
        RiskReport report = new RiskReport();
        report.setId(reportId);
        report.setProjectId(projectId);
        report.setReportType(reportType);
        report.setRiskLevel("HIGH");
        report.setRiskScore(80);
        report.setSummary("Risk summary");
        report.setRiskItems("[\"risk1\"]");
        report.setSuggestions("[\"fix1\"]");
        report.setPayloadJson("{}");
        riskReportService.save(report);
    }

    private void setupProject(Long projectId) {
        ProjectInfo project = new ProjectInfo();
        project.setId(projectId);
        project.setProjectName("RedisDisabledProject");
        project.setProjectPath("/tmp/redis-disabled-test");
        project.setProjectType("JAVA_MAVEN");
        project.setTechStack("[\"Java\"]");
        projectInfoService.save(project);
    }
}

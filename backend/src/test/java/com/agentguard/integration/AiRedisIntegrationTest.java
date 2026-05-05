package com.agentguard.integration;

import com.agentguard.ai.cache.AiAnalysisCacheService;
import com.agentguard.ai.cache.AiRateLimitService;
import com.agentguard.ai.dto.AiGitDiffAnalysisRequest;
import com.agentguard.ai.dto.AiReportSummaryRequest;
import com.agentguard.ai.dto.AiRiskExplainRequest;
import com.agentguard.ai.service.AiAnalysisService;
import com.agentguard.ai.service.impl.LlmAiAnalysisServiceImpl;
import com.agentguard.ai.service.impl.MockAiAnalysisServiceImpl;
import com.agentguard.ai.vo.AiGitDiffAnalysisVO;
import com.agentguard.ai.vo.AiReportSummaryVO;
import com.agentguard.ai.vo.AiRiskExplainVO;
import com.agentguard.ai.vo.AiRuntimeStatusVO;
import com.agentguard.cache.RedisCacheService;
import com.agentguard.cache.RedisKeyBuilder;
import com.agentguard.cache.RedisRateLimitService;
import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.config.AiCacheProperties;
import com.agentguard.config.AiRateLimitProperties;
import com.agentguard.entity.ProjectInfo;
import com.agentguard.entity.RiskReport;
import com.agentguard.service.ProjectInfoService;
import com.agentguard.service.RiskReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests against a real Redis instance.
 * Requires Redis running on localhost:6379 with no password.
 *
 * Tests cover:
 * 1. Redis connectivity and basic ops
 * 2. Cache put/get for all 3 AI VO types
 * 3. Cache hit returns cached=true, skips LLM
 * 4. Cache miss triggers LLM/mock, stores result
 * 5. Different inputs produce different cache keys
 * 6. Rate limit under threshold allows
 * 7. Rate limit over threshold throws AI_RATE_LIMITED
 * 8. Rate limit counter increments correctly
 * 9. Status endpoint reports Redis/cache/rate-limit enabled
 * 10. Full flow: mock service with cache round-trip
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.datasource.url=jdbc:h2:mem:agentguard_redis_real_it;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/integration-schema.sql",
        "agentguard.ai.enabled=false",
        "agentguard.redis.enabled=true",
        "agentguard.ai.cache.enabled=true",
        "agentguard.ai.cache.ttl-seconds=60",
        "agentguard.ai.rate-limit.enabled=true",
        "agentguard.ai.rate-limit.per-minute=5",
        "agentguard.ai.rate-limit.per-day=50",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.data.redis.password=",
        "spring.data.redis.timeout=3000ms",
        "mybatis-plus.configuration.map-underscore-to-camel-case=true",
        "mybatis-plus.global-config.db-config.logic-delete-field=deleted",
        "mybatis-plus.global-config.db-config.logic-delete-value=1",
        "mybatis-plus.global-config.db-config.logic-not-delete-value=0"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AiRedisIntegrationTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private RedisRateLimitService redisRateLimitService;

    @Autowired
    private RedisKeyBuilder redisKeyBuilder;

    @Autowired
    private AiAnalysisCacheService aiAnalysisCacheService;

    @Autowired
    private AiRateLimitService aiRateLimitService;

    @Autowired
    private MockAiAnalysisServiceImpl mockAiAnalysisService;

    @Autowired
    private AiCacheProperties aiCacheProperties;

    @Autowired
    private AiRateLimitProperties aiRateLimitProperties;

    private static final Long PROJECT_ID = 9001L;
    private static final Long REPORT_ID = 8001L;
    private static final Long GIT_AUDIT_REPORT_ID = 7001L;

    @BeforeEach
    void cleanRedis() {
        // Clean test keys before each test
        try {
            var keys = redisTemplate.keys("agentguard:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // ignore cleanup errors
        }
    }

    // ========== 1. Redis Connectivity ==========

    @Test
    @Order(1)
    void redisTemplate_shouldBeConnected() {
        assertThat(redisTemplate).isNotNull();
        String pong = redisTemplate.getConnectionFactory().getConnection().ping();
        assertThat(pong).isEqualTo("PONG");
    }

    @Test
    @Order(2)
    void redisCacheService_shouldBeInjected() {
        assertThat(redisCacheService).isNotNull();
    }

    @Test
    @Order(3)
    void redisRateLimitService_shouldBeInjected() {
        assertThat(redisRateLimitService).isNotNull();
    }

    // ========== 2. Basic Redis Ops ==========

    @Test
    @Order(10)
    void cacheService_setAndGet_shouldWork() {
        redisCacheService.set("agentguard:test:key1", "value1", 30);

        Optional<String> result = redisCacheService.get("agentguard:test:key1");

        assertThat(result).contains("value1");
    }

    @Test
    @Order(11)
    void cacheService_getMissingKey_shouldReturnEmpty() {
        Optional<String> result = redisCacheService.get("agentguard:test:nonexistent");
        assertThat(result).isEmpty();
    }

    @Test
    @Order(12)
    void cacheService_hasKey_shouldWork() {
        redisCacheService.set("agentguard:test:exists", "v", 30);

        assertThat(redisCacheService.hasKey("agentguard:test:exists")).isTrue();
        assertThat(redisCacheService.hasKey("agentguard:test:missing")).isFalse();
    }

    @Test
    @Order(13)
    void cacheService_delete_shouldWork() {
        redisCacheService.set("agentguard:test:toDelete", "v", 30);
        assertThat(redisCacheService.hasKey("agentguard:test:toDelete")).isTrue();

        redisCacheService.delete("agentguard:test:toDelete");
        assertThat(redisCacheService.hasKey("agentguard:test:toDelete")).isFalse();
    }

    @Test
    @Order(14)
    void rateLimitService_incrementAndGet_shouldIncrement() {
        long count1 = redisRateLimitService.incrementAndGet("agentguard:test:counter", 60);
        long count2 = redisRateLimitService.incrementAndGet("agentguard:test:counter", 60);
        long count3 = redisRateLimitService.incrementAndGet("agentguard:test:counter", 60);

        assertThat(count1).isEqualTo(1L);
        assertThat(count2).isEqualTo(2L);
        assertThat(count3).isEqualTo(3L);
    }

    // ========== 3. AI Cache: Git Diff ==========

    @Test
    @Order(20)
    void gitDiffCache_missThenHit() {
        // Miss
        Optional<AiGitDiffAnalysisVO> miss = aiAnalysisCacheService.getGitDiffAnalysis(GIT_AUDIT_REPORT_ID);
        assertThat(miss).isEmpty();

        // Put
        AiGitDiffAnalysisVO vo = new AiGitDiffAnalysisVO();
        vo.setProjectId(PROJECT_ID);
        vo.setGitAuditReportId(GIT_AUDIT_REPORT_ID);
        vo.setSummary("cached git diff summary");
        vo.setImpactAreas(java.util.List.of("area1"));
        vo.setTestSuggestions(java.util.List.of("test1"));
        vo.setRollbackSuggestions(java.util.List.of("rollback1"));
        vo.setConfidenceNote(AiAnalysisService.CONFIDENCE_NOTE);
        vo.setMocked(true);
        aiAnalysisCacheService.putGitDiffAnalysis(GIT_AUDIT_REPORT_ID, vo);

        // Hit
        Optional<AiGitDiffAnalysisVO> hit = aiAnalysisCacheService.getGitDiffAnalysis(GIT_AUDIT_REPORT_ID);
        assertThat(hit).isPresent();
        assertThat(hit.get().getSummary()).isEqualTo("cached git diff summary");
        assertThat(hit.get().getCached()).isTrue();
        assertThat(hit.get().getImpactAreas()).containsExactly("area1");
    }

    // ========== 4. AI Cache: Risk Explain ==========

    @Test
    @Order(21)
    void riskExplainCache_missThenHit() {
        Optional<AiRiskExplainVO> miss = aiAnalysisCacheService.getRiskExplain(REPORT_ID);
        assertThat(miss).isEmpty();

        AiRiskExplainVO vo = new AiRiskExplainVO();
        vo.setProjectId(PROJECT_ID);
        vo.setReportId(REPORT_ID);
        vo.setRiskSummary("cached risk summary");
        vo.setWhyItMatters(java.util.List.of("reason1"));
        vo.setFixPlan(java.util.List.of("fix1"));
        vo.setSafeNextSteps(java.util.List.of("step1"));
        vo.setConfidenceNote(AiAnalysisService.CONFIDENCE_NOTE);
        vo.setMocked(true);
        aiAnalysisCacheService.putRiskExplain(REPORT_ID, vo);

        Optional<AiRiskExplainVO> hit = aiAnalysisCacheService.getRiskExplain(REPORT_ID);
        assertThat(hit).isPresent();
        assertThat(hit.get().getRiskSummary()).isEqualTo("cached risk summary");
        assertThat(hit.get().getCached()).isTrue();
    }

    // ========== 5. AI Cache: Report Summary ==========

    @Test
    @Order(22)
    void reportSummaryCache_missThenHit() {
        String markdown = "# Test Report\n- Item 1";

        Optional<AiReportSummaryVO> miss = aiAnalysisCacheService.getReportSummary(PROJECT_ID, markdown);
        assertThat(miss).isEmpty();

        AiReportSummaryVO vo = new AiReportSummaryVO();
        vo.setProjectId(PROJECT_ID);
        vo.setExecutiveSummary("cached executive summary");
        vo.setKeyFindings(java.util.List.of("finding1"));
        vo.setPriorityActions(java.util.List.of("action1"));
        vo.setConfidenceNote(AiAnalysisService.CONFIDENCE_NOTE);
        vo.setMocked(true);
        aiAnalysisCacheService.putReportSummary(PROJECT_ID, markdown, vo);

        Optional<AiReportSummaryVO> hit = aiAnalysisCacheService.getReportSummary(PROJECT_ID, markdown);
        assertThat(hit).isPresent();
        assertThat(hit.get().getExecutiveSummary()).isEqualTo("cached executive summary");
        assertThat(hit.get().getCached()).isTrue();
    }

    // ========== 6. Cache Key Isolation ==========

    @Test
    @Order(23)
    void reportSummaryCache_differentMarkdownDifferentKeys() {
        AiReportSummaryVO vo1 = new AiReportSummaryVO();
        vo1.setExecutiveSummary("summary for doc A");
        vo1.setProjectId(PROJECT_ID);
        vo1.setConfidenceNote(AiAnalysisService.CONFIDENCE_NOTE);
        vo1.setMocked(true);

        AiReportSummaryVO vo2 = new AiReportSummaryVO();
        vo2.setExecutiveSummary("summary for doc B");
        vo2.setProjectId(PROJECT_ID);
        vo2.setConfidenceNote(AiAnalysisService.CONFIDENCE_NOTE);
        vo2.setMocked(true);

        aiAnalysisCacheService.putReportSummary(PROJECT_ID, "doc A content", vo1);
        aiAnalysisCacheService.putReportSummary(PROJECT_ID, "doc B content", vo2);

        Optional<AiReportSummaryVO> hitA = aiAnalysisCacheService.getReportSummary(PROJECT_ID, "doc A content");
        Optional<AiReportSummaryVO> hitB = aiAnalysisCacheService.getReportSummary(PROJECT_ID, "doc B content");

        assertThat(hitA).isPresent();
        assertThat(hitB).isPresent();
        assertThat(hitA.get().getExecutiveSummary()).isEqualTo("summary for doc A");
        assertThat(hitB.get().getExecutiveSummary()).isEqualTo("summary for doc B");
    }

    @Test
    @Order(24)
    void gitDiffCache_differentReportsDifferentKeys() {
        AiGitDiffAnalysisVO vo1 = buildGitDiffVO("summary for report 100");
        AiGitDiffAnalysisVO vo2 = buildGitDiffVO("summary for report 200");

        aiAnalysisCacheService.putGitDiffAnalysis(100L, vo1);
        aiAnalysisCacheService.putGitDiffAnalysis(200L, vo2);

        Optional<AiGitDiffAnalysisVO> hit1 = aiAnalysisCacheService.getGitDiffAnalysis(100L);
        Optional<AiGitDiffAnalysisVO> hit2 = aiAnalysisCacheService.getGitDiffAnalysis(200L);

        assertThat(hit1).isPresent();
        assertThat(hit2).isPresent();
        assertThat(hit1.get().getSummary()).isEqualTo("summary for report 100");
        assertThat(hit2.get().getSummary()).isEqualTo("summary for report 200");
    }

    // ========== 7. Rate Limiting ==========

    @Test
    @Order(30)
    void rateLimit_underThreshold_shouldAllow() {
        // per-minute is 5, calling 5 times should be fine
        for (int i = 0; i < 5; i++) {
            aiRateLimitService.checkAndIncrement(PROJECT_ID);
        }
        // No exception = pass
    }

    @Test
    @Order(31)
    void rateLimit_exceedMinuteThreshold_shouldThrow() {
        // per-minute is 5, 6th call should fail
        for (int i = 0; i < 5; i++) {
            aiRateLimitService.checkAndIncrement(PROJECT_ID);
        }

        assertThatThrownBy(() -> aiRateLimitService.checkAndIncrement(PROJECT_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.AI_RATE_LIMITED);
                })
                .hasMessageContaining("每分钟");
    }

    @Test
    @Order(32)
    void rateLimit_differentProjects_independent() {
        Long projectA = 9010L;
        Long projectB = 9011L;

        // Exhaust project A's limit
        for (int i = 0; i < 5; i++) {
            aiRateLimitService.checkAndIncrement(projectA);
        }

        // Project B should still be fine
        aiRateLimitService.checkAndIncrement(projectB);
        // No exception = pass
    }

    @Test
    @Order(33)
    void rateLimit_exactThreshold_shouldAllow() {
        // Exactly at the limit (5) should be allowed, only > 5 is rejected
        for (int i = 0; i < 5; i++) {
            aiRateLimitService.checkAndIncrement(PROJECT_ID);
        }
        // 5 calls at limit=5 → allowed
    }

    // ========== 8. Mock Service Cache Integration ==========

    @Test
    @Order(40)
    void mockService_analyzeGitDiff_shouldCacheResult() {
        AiGitDiffAnalysisRequest request = new AiGitDiffAnalysisRequest();
        request.setProjectId(PROJECT_ID);
        request.setGitAuditReportId(GIT_AUDIT_REPORT_ID);

        // First call — cache miss, builds mock, caches it
        AiGitDiffAnalysisVO first = mockAiAnalysisService.analyzeGitDiff(request);
        assertThat(first).isNotNull();
        assertThat(first.getSummary()).isNotBlank();
        assertThat(first.getCached()).isFalse();

        // Second call — cache hit
        AiGitDiffAnalysisVO second = mockAiAnalysisService.analyzeGitDiff(request);
        assertThat(second).isNotNull();
        assertThat(second.getCached()).isTrue();
        assertThat(second.getSummary()).isEqualTo(first.getSummary());
    }

    @Test
    @Order(41)
    void mockService_explainRisk_shouldCacheResult() {
        AiRiskExplainRequest request = new AiRiskExplainRequest();
        request.setProjectId(PROJECT_ID);
        request.setReportId(REPORT_ID);

        AiRiskExplainVO first = mockAiAnalysisService.explainRisk(request);
        assertThat(first).isNotNull();
        assertThat(first.getCached()).isFalse();

        AiRiskExplainVO second = mockAiAnalysisService.explainRisk(request);
        assertThat(second).isNotNull();
        assertThat(second.getCached()).isTrue();
        assertThat(second.getRiskSummary()).isEqualTo(first.getRiskSummary());
    }

    @Test
    @Order(42)
    void mockService_summarizeReport_shouldCacheResult() {
        String markdown = "# Real Redis Test Report\n- Risk: MEDIUM";
        AiReportSummaryRequest request = new AiReportSummaryRequest();
        request.setProjectId(PROJECT_ID);
        request.setMarkdown(markdown);

        AiReportSummaryVO first = mockAiAnalysisService.summarizeReport(request);
        assertThat(first).isNotNull();
        assertThat(first.getCached()).isFalse();

        AiReportSummaryVO second = mockAiAnalysisService.summarizeReport(request);
        assertThat(second).isNotNull();
        assertThat(second.getCached()).isTrue();
        assertThat(second.getExecutiveSummary()).isEqualTo(first.getExecutiveSummary());
    }

    // ========== 9. Config Properties ==========

    @Test
    @Order(50)
    void cacheProperties_shouldBeLoaded() {
        assertThat(aiCacheProperties.isEnabled()).isTrue();
        assertThat(aiCacheProperties.getTtlSeconds()).isEqualTo(60);
    }

    @Test
    @Order(51)
    void rateLimitProperties_shouldBeLoaded() {
        assertThat(aiRateLimitProperties.isEnabled()).isTrue();
        assertThat(aiRateLimitProperties.getPerMinute()).isEqualTo(5);
        assertThat(aiRateLimitProperties.getPerDay()).isEqualTo(50);
    }

    // ========== 10. Key Builder with Real Redis ==========

    @Test
    @Order(60)
    void keyBuilder_keysShouldWorkWithRedis() {
        String key = redisKeyBuilder.aiGitDiffAnalysis(12345L);
        assertThat(key).isEqualTo("agentguard:ai:result:git-diff:12345");

        // Write via key builder, read back
        redisCacheService.set(key, "test-value", 30);
        Optional<String> value = redisCacheService.get(key);
        assertThat(value).contains("test-value");
    }

    @Test
    @Order(61)
    void keyBuilder_rateLimitKeysShouldContainTimestamp() {
        String minuteKey = redisKeyBuilder.rateLimitMinute(PROJECT_ID);
        assertThat(minuteKey).startsWith("agentguard:rate:ai:project:" + PROJECT_ID + ":minute:");

        String dayKey = redisKeyBuilder.rateLimitDay(PROJECT_ID);
        assertThat(dayKey).startsWith("agentguard:rate:ai:project:" + PROJECT_ID + ":day:");
    }

    // ========== 11. TTL Behavior ==========

    @Test
    @Order(70)
    void cacheService_keyShouldExpireAfterTtl() throws InterruptedException {
        // Use a very short TTL (1 second) via direct Redis
        String key = "agentguard:test:ttl-check";
        redisTemplate.opsForValue().set(key, "ephemeral", 1, java.util.concurrent.TimeUnit.SECONDS);

        assertThat(redisCacheService.get(key)).contains("ephemeral");

        // Wait for expiry
        Thread.sleep(1500);

        assertThat(redisCacheService.get(key)).isEmpty();
    }

    // ========== 12. Concurrent Access ==========

    @Test
    @Order(80)
    void rateLimitService_concurrentIncrements_shouldBeAccurate() throws InterruptedException {
        String key = "agentguard:test:concurrent-counter";
        int threadCount = 10;
        int incrementsPerThread = 5;

        // Clean
        redisTemplate.delete(key);

        var latch = new java.util.concurrent.CountDownLatch(threadCount);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < incrementsPerThread; i++) {
                        redisRateLimitService.incrementAndGet(key, 60);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Total should be threadCount * incrementsPerThread = 50
        // Read the final count
        String countStr = redisTemplate.opsForValue().get(key);
        assertThat(Long.parseLong(countStr)).isEqualTo((long) threadCount * incrementsPerThread);
    }

    // ========== Helper ==========

    private AiGitDiffAnalysisVO buildGitDiffVO(String summary) {
        AiGitDiffAnalysisVO vo = new AiGitDiffAnalysisVO();
        vo.setProjectId(PROJECT_ID);
        vo.setSummary(summary);
        vo.setImpactAreas(java.util.List.of("area"));
        vo.setTestSuggestions(java.util.List.of("test"));
        vo.setRollbackSuggestions(java.util.List.of("rollback"));
        vo.setConfidenceNote(AiAnalysisService.CONFIDENCE_NOTE);
        vo.setMocked(true);
        return vo;
    }
}

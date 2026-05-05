package com.agentguard.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedisKeyBuilderTest {

    private RedisKeyBuilder keyBuilder;

    @BeforeEach
    void setUp() {
        keyBuilder = new RedisKeyBuilder();
    }

    @Test
    void aiGitDiffAnalysis_shouldContainReportId() {
        String key = keyBuilder.aiGitDiffAnalysis(42L);
        assertThat(key).isEqualTo("agentguard:ai:result:git-diff:42");
    }

    @Test
    void aiRiskExplain_shouldContainReportId() {
        String key = keyBuilder.aiRiskExplain(99L);
        assertThat(key).isEqualTo("agentguard:ai:result:risk-explain:99");
    }

    @Test
    void aiReportSummary_shouldContainProjectIdAndHash() {
        String key = keyBuilder.aiReportSummary(1L, "# Report");
        assertThat(key).startsWith("agentguard:ai:result:report-summary:1:");
        assertThat(key).doesNotContain("# Report");
        // SHA-256 hex is 64 chars
        String hash = key.substring(key.lastIndexOf(":") + 1);
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void aiReportSummary_sameInputSameKey() {
        String key1 = keyBuilder.aiReportSummary(1L, "same content");
        String key2 = keyBuilder.aiReportSummary(1L, "same content");
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void aiReportSummary_differentInputDifferentKey() {
        String key1 = keyBuilder.aiReportSummary(1L, "content A");
        String key2 = keyBuilder.aiReportSummary(1L, "content B");
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void aiReportSummary_differentProjectSameContentDifferentKey() {
        String key1 = keyBuilder.aiReportSummary(1L, "same content");
        String key2 = keyBuilder.aiReportSummary(2L, "same content");
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void rateLimitMinute_shouldContainProjectIdAndTimestamp() {
        String key = keyBuilder.rateLimitMinute(5L);
        assertThat(key).startsWith("agentguard:rate:ai:project:5:minute:");
        // Timestamp format: yyyyMMddHHmm (12 digits)
        String timestamp = key.substring(key.lastIndexOf(":") + 1);
        assertThat(timestamp).hasSize(12);
        assertThat(timestamp).matches("\\d{12}");
    }

    @Test
    void rateLimitDay_shouldContainProjectIdAndDate() {
        String key = keyBuilder.rateLimitDay(5L);
        assertThat(key).startsWith("agentguard:rate:ai:project:5:day:");
        // Date format: yyyyMMdd (8 digits)
        String date = key.substring(key.lastIndexOf(":") + 1);
        assertThat(date).hasSize(8);
        assertThat(date).matches("\\d{8}");
    }

    @Test
    void sha256_shouldReturnConsistentHash() {
        String hash1 = RedisKeyBuilder.sha256("test input");
        String hash2 = RedisKeyBuilder.sha256("test input");
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void sha256_shouldHandleNull() {
        String hash = RedisKeyBuilder.sha256(null);
        assertThat(hash).hasSize(64);
        assertThat(hash).isEqualTo(RedisKeyBuilder.sha256(""));
    }

    @Test
    void sha256_shouldNotContainSensitiveData() {
        String secret = "api_key=sk-secret12345";
        String hash = RedisKeyBuilder.sha256(secret);
        assertThat(hash).doesNotContain("sk-secret12345");
        assertThat(hash).doesNotContain("api_key");
    }

    @Test
    void keys_shouldNotContainSensitiveData() {
        String key = keyBuilder.aiReportSummary(1L, "api_key=sk-secret");
        assertThat(key).doesNotContain("sk-secret");
    }
}

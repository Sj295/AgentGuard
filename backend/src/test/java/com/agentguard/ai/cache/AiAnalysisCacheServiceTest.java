package com.agentguard.ai.cache;

import com.agentguard.ai.vo.AiGitDiffAnalysisVO;
import com.agentguard.ai.vo.AiReportSummaryVO;
import com.agentguard.ai.vo.AiRiskExplainVO;
import com.agentguard.cache.RedisCacheService;
import com.agentguard.cache.RedisKeyBuilder;
import com.agentguard.config.AiCacheProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAnalysisCacheServiceTest {

    private RedisCacheService redisCacheService;
    private RedisKeyBuilder redisKeyBuilder;
    private AiCacheProperties properties;
    private ObjectMapper objectMapper;
    private AiAnalysisCacheService service;

    @BeforeEach
    void setUp() {
        redisCacheService = Mockito.mock(RedisCacheService.class);
        redisKeyBuilder = new RedisKeyBuilder();
        properties = new AiCacheProperties();
        properties.setTtlSeconds(3600);
        objectMapper = new ObjectMapper();
        service = new AiAnalysisCacheService(redisCacheService, redisKeyBuilder, properties, objectMapper);
    }

    // ========== Git Diff Analysis Cache ==========

    @Test
    void getGitDiffAnalysis_shouldReturnCachedValue() {
        AiGitDiffAnalysisVO cached = new AiGitDiffAnalysisVO();
        cached.setSummary("cached summary");
        cached.setMocked(false);
        String json = serialize(cached);
        when(redisCacheService.get(anyString())).thenReturn(Optional.of(json));

        Optional<AiGitDiffAnalysisVO> result = service.getGitDiffAnalysis(42L);

        assertThat(result).isPresent();
        assertThat(result.get().getSummary()).isEqualTo("cached summary");
        assertThat(result.get().getCached()).isTrue();
    }

    @Test
    void getGitDiffAnalysis_shouldReturnEmptyWhenNotCached() {
        when(redisCacheService.get(anyString())).thenReturn(Optional.empty());

        Optional<AiGitDiffAnalysisVO> result = service.getGitDiffAnalysis(42L);

        assertThat(result).isEmpty();
    }

    @Test
    void getGitDiffAnalysis_shouldReturnEmptyOnDeserializationFailure() {
        when(redisCacheService.get(anyString())).thenReturn(Optional.of("not valid json {{{"));

        Optional<AiGitDiffAnalysisVO> result = service.getGitDiffAnalysis(42L);

        assertThat(result).isEmpty();
    }

    @Test
    void putGitDiffAnalysis_shouldStoreWithCorrectKey() {
        AiGitDiffAnalysisVO vo = new AiGitDiffAnalysisVO();
        vo.setSummary("test");

        service.putGitDiffAnalysis(42L, vo);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisCacheService).set(keyCaptor.capture(), anyString(), eq(3600L));
        assertThat(keyCaptor.getValue()).isEqualTo("agentguard:ai:result:git-diff:42");
    }

    // ========== Risk Explain Cache ==========

    @Test
    void getRiskExplain_shouldReturnCachedValue() {
        AiRiskExplainVO cached = new AiRiskExplainVO();
        cached.setRiskSummary("cached risk");
        cached.setMocked(false);
        String json = serialize(cached);
        when(redisCacheService.get(anyString())).thenReturn(Optional.of(json));

        Optional<AiRiskExplainVO> result = service.getRiskExplain(99L);

        assertThat(result).isPresent();
        assertThat(result.get().getRiskSummary()).isEqualTo("cached risk");
        assertThat(result.get().getCached()).isTrue();
    }

    @Test
    void getRiskExplain_shouldReturnEmptyWhenNotCached() {
        when(redisCacheService.get(anyString())).thenReturn(Optional.empty());

        Optional<AiRiskExplainVO> result = service.getRiskExplain(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void putRiskExplain_shouldStoreWithCorrectKey() {
        AiRiskExplainVO vo = new AiRiskExplainVO();
        vo.setRiskSummary("test");

        service.putRiskExplain(99L, vo);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisCacheService).set(keyCaptor.capture(), anyString(), eq(3600L));
        assertThat(keyCaptor.getValue()).isEqualTo("agentguard:ai:result:risk-explain:99");
    }

    // ========== Report Summary Cache ==========

    @Test
    void getReportSummary_shouldReturnCachedValue() {
        AiReportSummaryVO cached = new AiReportSummaryVO();
        cached.setExecutiveSummary("cached exec");
        cached.setMocked(false);
        String json = serialize(cached);
        when(redisCacheService.get(anyString())).thenReturn(Optional.of(json));

        Optional<AiReportSummaryVO> result = service.getReportSummary(1L, "# Report");

        assertThat(result).isPresent();
        assertThat(result.get().getExecutiveSummary()).isEqualTo("cached exec");
        assertThat(result.get().getCached()).isTrue();
    }

    @Test
    void getReportSummary_shouldReturnEmptyWhenNotCached() {
        when(redisCacheService.get(anyString())).thenReturn(Optional.empty());

        Optional<AiReportSummaryVO> result = service.getReportSummary(1L, "# Report");

        assertThat(result).isEmpty();
    }

    @Test
    void putReportSummary_shouldUseSha256InKey() {
        AiReportSummaryVO vo = new AiReportSummaryVO();
        vo.setExecutiveSummary("test");

        service.putReportSummary(1L, "# Report Content", vo);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisCacheService).set(keyCaptor.capture(), anyString(), eq(3600L));
        String key = keyCaptor.getValue();
        assertThat(key).startsWith("agentguard:ai:result:report-summary:1:");
        assertThat(key).doesNotContain("# Report Content");
    }

    // ========== Key Consistency ==========

    @Test
    void getAndPut_shouldUseConsistentKeys() {
        // Put with specific inputs
        AiGitDiffAnalysisVO vo = new AiGitDiffAnalysisVO();
        vo.setSummary("test");

        service.putGitDiffAnalysis(42L, vo);

        ArgumentCaptor<String> putKey = ArgumentCaptor.forClass(String.class);
        verify(redisCacheService).set(putKey.capture(), anyString(), anyLong());

        // Get with same inputs
        when(redisCacheService.get(anyString())).thenReturn(Optional.empty());
        service.getGitDiffAnalysis(42L);

        ArgumentCaptor<String> getKey = ArgumentCaptor.forClass(String.class);
        verify(redisCacheService).get(getKey.capture());

        assertThat(putKey.getValue()).isEqualTo(getKey.getValue());
    }

    @Test
    void getReportSummary_sameInputsSameKey() {
        when(redisCacheService.get(anyString())).thenReturn(Optional.empty());

        service.getReportSummary(1L, "content");
        ArgumentCaptor<String> key1 = ArgumentCaptor.forClass(String.class);
        verify(redisCacheService).get(key1.capture());

        service.getReportSummary(1L, "content");
        ArgumentCaptor<String> key2 = ArgumentCaptor.forClass(String.class);
        verify(redisCacheService, Mockito.times(2)).get(key2.capture());

        assertThat(key1.getValue()).isEqualTo(key2.getValue());
    }

    // ========== TTL ==========

    @Test
    void putGitDiffAnalysis_shouldUseConfiguredTtl() {
        properties.setTtlSeconds(7200);
        AiGitDiffAnalysisVO vo = new AiGitDiffAnalysisVO();
        vo.setSummary("test");

        service.putGitDiffAnalysis(42L, vo);

        verify(redisCacheService).set(anyString(), anyString(), eq(7200L));
    }

    // ========== Serialization Round-trip ==========

    @Test
    void putAndGet_shouldPreserveAllFields() {
        AiGitDiffAnalysisVO original = new AiGitDiffAnalysisVO();
        original.setSummary("full round trip");
        original.setImpactAreas(List.of("area1", "area2"));
        original.setTestSuggestions(List.of("test1"));
        original.setRollbackSuggestions(List.of("rollback1"));
        original.setConfidenceNote("note");
        original.setMocked(false);

        // Serialize and store
        String json = serialize(original);
        when(redisCacheService.get(anyString())).thenReturn(Optional.of(json));

        Optional<AiGitDiffAnalysisVO> result = service.getGitDiffAnalysis(1L);

        assertThat(result).isPresent();
        AiGitDiffAnalysisVO vo = result.get();
        assertThat(vo.getSummary()).isEqualTo("full round trip");
        assertThat(vo.getImpactAreas()).containsExactly("area1", "area2");
        assertThat(vo.getTestSuggestions()).containsExactly("test1");
        assertThat(vo.getRollbackSuggestions()).containsExactly("rollback1");
        assertThat(vo.getConfidenceNote()).isEqualTo("note");
        assertThat(vo.getCached()).isTrue();
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

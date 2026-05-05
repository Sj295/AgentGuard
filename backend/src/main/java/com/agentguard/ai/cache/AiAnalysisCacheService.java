package com.agentguard.ai.cache;

import com.agentguard.ai.vo.AiGitDiffAnalysisVO;
import com.agentguard.ai.vo.AiReportSummaryVO;
import com.agentguard.ai.vo.AiRiskExplainVO;
import com.agentguard.cache.RedisCacheService;
import com.agentguard.cache.RedisKeyBuilder;
import com.agentguard.config.AiCacheProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@ConditionalOnProperty(name = {"agentguard.redis.enabled", "agentguard.ai.cache.enabled"}, havingValue = "true")
public class AiAnalysisCacheService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisCacheService.class);

    private final RedisCacheService redisCacheService;
    private final RedisKeyBuilder redisKeyBuilder;
    private final AiCacheProperties properties;
    private final ObjectMapper objectMapper;

    public AiAnalysisCacheService(RedisCacheService redisCacheService,
                                  RedisKeyBuilder redisKeyBuilder,
                                  AiCacheProperties properties,
                                  ObjectMapper objectMapper) {
        this.redisCacheService = redisCacheService;
        this.redisKeyBuilder = redisKeyBuilder;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Optional<AiGitDiffAnalysisVO> getGitDiffAnalysis(Long gitAuditReportId) {
        String key = redisKeyBuilder.aiGitDiffAnalysis(gitAuditReportId);
        return redisCacheService.get(key)
                .flatMap(json -> deserialize(json, AiGitDiffAnalysisVO.class))
                .map(vo -> {
                    vo.setCached(true);
                    return vo;
                });
    }

    public void putGitDiffAnalysis(Long gitAuditReportId, AiGitDiffAnalysisVO vo) {
        String key = redisKeyBuilder.aiGitDiffAnalysis(gitAuditReportId);
        serialize(vo).ifPresent(json -> redisCacheService.set(key, json, properties.getTtlSeconds()));
    }

    public Optional<AiRiskExplainVO> getRiskExplain(Long reportId) {
        String key = redisKeyBuilder.aiRiskExplain(reportId);
        return redisCacheService.get(key)
                .flatMap(json -> deserialize(json, AiRiskExplainVO.class))
                .map(vo -> {
                    vo.setCached(true);
                    return vo;
                });
    }

    public void putRiskExplain(Long reportId, AiRiskExplainVO vo) {
        String key = redisKeyBuilder.aiRiskExplain(reportId);
        serialize(vo).ifPresent(json -> redisCacheService.set(key, json, properties.getTtlSeconds()));
    }

    public Optional<AiReportSummaryVO> getReportSummary(Long projectId, String markdown) {
        String key = redisKeyBuilder.aiReportSummary(projectId, markdown);
        return redisCacheService.get(key)
                .flatMap(json -> deserialize(json, AiReportSummaryVO.class))
                .map(vo -> {
                    vo.setCached(true);
                    return vo;
                });
    }

    public void putReportSummary(Long projectId, String markdown, AiReportSummaryVO vo) {
        String key = redisKeyBuilder.aiReportSummary(projectId, markdown);
        serialize(vo).ifPresent(json -> redisCacheService.set(key, json, properties.getTtlSeconds()));
    }

    private <T> Optional<T> deserialize(String json, Class<T> clazz) {
        try {
            return Optional.ofNullable(objectMapper.readValue(json, clazz));
        } catch (JsonProcessingException e) {
            log.warn("Cache deserialization failed for {}: {}", clazz.getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> serialize(Object obj) {
        try {
            return Optional.ofNullable(objectMapper.writeValueAsString(obj));
        } catch (JsonProcessingException e) {
            log.warn("Cache serialization failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}

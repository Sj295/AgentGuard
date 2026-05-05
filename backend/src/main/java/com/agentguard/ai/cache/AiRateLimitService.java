package com.agentguard.ai.cache;

import com.agentguard.cache.RedisKeyBuilder;
import com.agentguard.cache.RedisRateLimitService;
import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.config.AiRateLimitProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = {"agentguard.redis.enabled", "agentguard.ai.rate-limit.enabled"}, havingValue = "true")
public class AiRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(AiRateLimitService.class);
    private static final long MINUTE_TTL_SECONDS = 120;
    private static final long DAY_TTL_SECONDS = 86400;

    private final RedisRateLimitService redisRateLimitService;
    private final RedisKeyBuilder redisKeyBuilder;
    private final AiRateLimitProperties properties;

    public AiRateLimitService(RedisRateLimitService redisRateLimitService,
                              RedisKeyBuilder redisKeyBuilder,
                              AiRateLimitProperties properties) {
        this.redisRateLimitService = redisRateLimitService;
        this.redisKeyBuilder = redisKeyBuilder;
        this.properties = properties;
    }

    public void checkAndIncrement(Long projectId) {
        String minuteKey = redisKeyBuilder.rateLimitMinute(projectId);
        long minuteCount = redisRateLimitService.incrementAndGet(minuteKey, MINUTE_TTL_SECONDS);
        if (minuteCount == 0) {
            log.warn("Redis unavailable for rate limit check, allowing request for project {}", projectId);
            return;
        }
        if (minuteCount > properties.getPerMinute()) {
            throw new BusinessException(ErrorCode.AI_RATE_LIMITED,
                    "每分钟 AI 调用次数上限 (" + properties.getPerMinute() + ") 已达到，请稍后再试");
        }

        String dayKey = redisKeyBuilder.rateLimitDay(projectId);
        long dayCount = redisRateLimitService.incrementAndGet(dayKey, DAY_TTL_SECONDS);
        if (dayCount == 0) {
            log.warn("Redis unavailable for daily rate limit check, allowing request for project {}", projectId);
            return;
        }
        if (dayCount > properties.getPerDay()) {
            throw new BusinessException(ErrorCode.AI_RATE_LIMITED,
                    "每天 AI 调用次数上限 (" + properties.getPerDay() + ") 已达到，请稍后再试");
        }
    }
}

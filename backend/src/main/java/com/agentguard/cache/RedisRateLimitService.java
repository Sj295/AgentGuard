package com.agentguard.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "agentguard.redis.enabled", havingValue = "true")
public class RedisRateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitService.class);

    private final RedisTemplate<String, String> redisTemplate;

    public RedisRateLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long incrementAndGet(String key, long ttlSeconds) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                return 0;
            }
            if (count == 1 && ttlSeconds > 0) {
                redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
            }
            return count;
        } catch (Exception e) {
            log.warn("Redis INCR failed for key={}: {}", key, e.getMessage());
            return 0;
        }
    }
}

package com.agentguard.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "agentguard.redis.enabled", havingValue = "true")
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private final RedisTemplate<String, String> redisTemplate;

    public RedisCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<String> get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.warn("Redis GET failed for key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public void set(String key, String value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis SET failed for key={}: {}", key, e.getMessage());
        }
    }

    public boolean hasKey(String key) {
        try {
            Boolean has = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(has);
        } catch (Exception e) {
            log.warn("Redis HAS_KEY failed for key={}: {}", key, e.getMessage());
            return false;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis DELETE failed for key={}: {}", key, e.getMessage());
        }
    }

    public boolean isAvailable() {
        try {
            RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return false;
            }
            try (RedisConnection connection = connectionFactory.getConnection()) {
                String pong = connection.ping();
                return "PONG".equalsIgnoreCase(pong);
            }
        } catch (Exception e) {
            log.warn("Redis PING failed: {}", e.getMessage());
            return false;
        }
    }
}

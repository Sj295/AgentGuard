package com.agentguard.ai.cache;

import com.agentguard.cache.RedisKeyBuilder;
import com.agentguard.cache.RedisRateLimitService;
import com.agentguard.common.BusinessException;
import com.agentguard.common.ErrorCode;
import com.agentguard.config.AiRateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AiRateLimitServiceTest {

    private RedisRateLimitService redisRateLimitService;
    private RedisKeyBuilder redisKeyBuilder;
    private AiRateLimitProperties properties;
    private AiRateLimitService service;

    private static final Long PROJECT_ID = 1L;

    @BeforeEach
    void setUp() {
        redisRateLimitService = Mockito.mock(RedisRateLimitService.class);
        redisKeyBuilder = new RedisKeyBuilder();
        properties = new AiRateLimitProperties();
        properties.setPerMinute(10);
        properties.setPerDay(200);
        service = new AiRateLimitService(redisRateLimitService, redisKeyBuilder, properties);
    }

    @Test
    void checkAndIncrement_shouldAllowWhenUnderLimits() {
        when(redisRateLimitService.incrementAndGet(anyString(), anyLong())).thenReturn(5L);

        // Should not throw
        service.checkAndIncrement(PROJECT_ID);
    }

    @Test
    void checkAndIncrement_shouldThrowWhenMinuteLimitExceeded() {
        // First call (minute) returns 11 (over limit of 10)
        when(redisRateLimitService.incrementAndGet(anyString(), anyLong()))
                .thenReturn(11L);

        assertThatThrownBy(() -> service.checkAndIncrement(PROJECT_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.AI_RATE_LIMITED);
                })
                .hasMessageContaining("每分钟");
    }

    @Test
    void checkAndIncrement_shouldThrowWhenDayLimitExceeded() {
        // First call (minute) is under limit, second call (day) exceeds
        when(redisRateLimitService.incrementAndGet(anyString(), anyLong()))
                .thenReturn(5L)   // minute: under limit
                .thenReturn(201L); // day: over limit of 200

        assertThatThrownBy(() -> service.checkAndIncrement(PROJECT_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException bex = (BusinessException) ex;
                    assertThat(bex.getErrorCode()).isEqualTo(ErrorCode.AI_RATE_LIMITED);
                })
                .hasMessageContaining("每天");
    }

    @Test
    void checkAndIncrement_shouldAllowWhenRedisUnavailable_minuteReturnsZero() {
        // Redis INCR returns 0 on failure (fail-open)
        when(redisRateLimitService.incrementAndGet(anyString(), anyLong())).thenReturn(0L);

        // Should not throw — fail-open
        service.checkAndIncrement(PROJECT_ID);
    }

    @Test
    void checkAndIncrement_shouldAllowWhenRedisUnavailable_dayReturnsZero() {
        // Minute check passes, day check returns 0 (Redis down)
        when(redisRateLimitService.incrementAndGet(anyString(), anyLong()))
                .thenReturn(5L)  // minute: ok
                .thenReturn(0L); // day: Redis unavailable

        // Should not throw — fail-open
        service.checkAndIncrement(PROJECT_ID);
    }

    @Test
    void checkAndIncrement_shouldAllowAtExactLimit() {
        // At exactly the limit (not over)
        when(redisRateLimitService.incrementAndGet(anyString(), anyLong()))
                .thenReturn(10L)  // minute: exactly at limit
                .thenReturn(200L); // day: exactly at limit

        // Should not throw — at limit is allowed, only over is rejected
        service.checkAndIncrement(PROJECT_ID);
    }

    @Test
    void checkAndIncrement_shouldCheckMinuteFirst() {
        // If minute is exceeded, day should not be checked
        when(redisRateLimitService.incrementAndGet(anyString(), anyLong()))
                .thenReturn(11L); // minute exceeded

        assertThatThrownBy(() -> service.checkAndIncrement(PROJECT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("每分钟");

        // Only one call to incrementAndGet (minute), day never checked
        Mockito.verify(redisRateLimitService, Mockito.times(1))
                .incrementAndGet(anyString(), anyLong());
    }
}

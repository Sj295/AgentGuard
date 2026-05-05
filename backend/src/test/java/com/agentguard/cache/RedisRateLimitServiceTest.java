package com.agentguard.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRateLimitServiceTest {

    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOps;
    private RedisRateLimitService service;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(RedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new RedisRateLimitService(redisTemplate);
    }

    @Test
    void incrementAndGet_shouldReturnIncrementedValue() {
        when(valueOps.increment("key1")).thenReturn(1L);

        long result = service.incrementAndGet("key1", 60);

        assertThat(result).isEqualTo(1L);
    }

    @Test
    void incrementAndGet_shouldSetTtlOnFirstIncrement() {
        when(valueOps.increment("key1")).thenReturn(1L);

        service.incrementAndGet("key1", 60);

        verify(redisTemplate).expire("key1", 60, TimeUnit.SECONDS);
    }

    @Test
    void incrementAndGet_shouldNotSetTtlOnSubsequentIncrements() {
        when(valueOps.increment("key1")).thenReturn(5L);

        service.incrementAndGet("key1", 60);

        verify(redisTemplate, Mockito.never()).expire(anyString(), anyLong(), any());
    }

    @Test
    void incrementAndGet_shouldReturnZeroOnRedisFailure() {
        when(valueOps.increment(anyString())).thenThrow(new RuntimeException("Connection refused"));

        long result = service.incrementAndGet("key1", 60);

        assertThat(result).isEqualTo(0L);
    }

    @Test
    void incrementAndGet_shouldReturnZeroWhenIncrementReturnsNull() {
        when(valueOps.increment("key1")).thenReturn(null);

        long result = service.incrementAndGet("key1", 60);

        assertThat(result).isEqualTo(0L);
    }

    @Test
    void incrementAndGet_shouldNotSetTtlWhenTtlIsZero() {
        when(valueOps.increment("key1")).thenReturn(1L);

        service.incrementAndGet("key1", 0);

        verify(redisTemplate, Mockito.never()).expire(anyString(), anyLong(), any());
    }
}

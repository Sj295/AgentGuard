package com.agentguard.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCacheServiceTest {

    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOps;
    private RedisCacheService service;

    @BeforeEach
    void setUp() {
        redisTemplate = Mockito.mock(RedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new RedisCacheService(redisTemplate);
    }

    @Test
    void get_shouldReturnValue() {
        when(valueOps.get("key1")).thenReturn("value1");

        Optional<String> result = service.get("key1");

        assertThat(result).contains("value1");
    }

    @Test
    void get_shouldReturnEmptyWhenKeyMissing() {
        when(valueOps.get("missing")).thenReturn(null);

        Optional<String> result = service.get("missing");

        assertThat(result).isEmpty();
    }

    @Test
    void get_shouldReturnEmptyOnRedisFailure() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("Connection refused"));

        Optional<String> result = service.get("key1");

        assertThat(result).isEmpty();
    }

    @Test
    void set_shouldCallRedisWithTtl() {
        service.set("key1", "value1", 60L);

        verify(valueOps).set("key1", "value1", 60L, TimeUnit.SECONDS);
    }

    @Test
    void set_shouldNotThrowOnRedisFailure() {
        Mockito.doThrow(new RuntimeException("Connection refused"))
                .when(valueOps).set(anyString(), anyString(), anyLong(), any());

        // Should not throw
        service.set("key1", "value1", 60L);
    }

    @Test
    void hasKey_shouldReturnTrueWhenKeyExists() {
        when(redisTemplate.hasKey("key1")).thenReturn(true);

        assertThat(service.hasKey("key1")).isTrue();
    }

    @Test
    void hasKey_shouldReturnFalseWhenKeyMissing() {
        when(redisTemplate.hasKey("missing")).thenReturn(false);

        assertThat(service.hasKey("missing")).isFalse();
    }

    @Test
    void hasKey_shouldReturnFalseOnRedisFailure() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Connection refused"));

        assertThat(service.hasKey("key1")).isFalse();
    }

    @Test
    void delete_shouldCallRedis() {
        service.delete("key1");

        verify(redisTemplate).delete("key1");
    }

    @Test
    void delete_shouldNotThrowOnRedisFailure() {
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Connection refused"));

        // Should not throw
        service.delete("key1");
    }

    @Test
    void isAvailable_shouldReturnTrueOnPong() {
        RedisConnectionFactory connectionFactory = Mockito.mock(RedisConnectionFactory.class);
        RedisConnection connection = Mockito.mock(RedisConnection.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.ping()).thenReturn("PONG");

        assertThat(service.isAvailable()).isTrue();
    }

    @Test
    void isAvailable_shouldReturnFalseOnRedisFailure() {
        RedisConnectionFactory connectionFactory = Mockito.mock(RedisConnectionFactory.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenThrow(new RuntimeException("Connection refused"));

        assertThat(service.isAvailable()).isFalse();
    }
}

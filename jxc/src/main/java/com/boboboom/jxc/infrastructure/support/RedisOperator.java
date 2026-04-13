package com.boboboom.jxc.infrastructure.support;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisOperator {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisOperator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    public String ping() {
        return redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
    }
}


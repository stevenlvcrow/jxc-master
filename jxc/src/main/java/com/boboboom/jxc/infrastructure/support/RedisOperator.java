package com.boboboom.jxc.infrastructure.support;

import java.time.Duration;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/** Redis 操作组件，封装缓存读写和连通性检查。 */
@Component
public class RedisOperator {

    private final RedisTemplate<String, Object> redisTemplate;

    /** Redis 操作组件，封装缓存读写和连通性检查。 */
    public RedisOperator(RedisTemplate<String, Object> redisTemplateValue) {
        this.redisTemplate = redisTemplateValue;
    }

    /** 写入 Redis 缓存值。 */
    public void set(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    /** 写入 Redis 缓存值。 */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /** 读取 Redis 缓存值。 */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /** 删除业务记录。 */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /** 检查 Redis 连接可用性。 */
    public String ping() {
        return redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
    }
}


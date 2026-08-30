package com.tns.mes.common.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisCacheService {
    private final RedisTemplate<String, Object> redis;
    public RedisCacheService(@Qualifier("mesRedisTemplate") RedisTemplate<String, Object> redis) { this.redis = redis; }
    public Object get(String key) { try { return redis.opsForValue().get(key); } catch (RuntimeException ex) { return null; } }
    public <T> T get(String key, Class<T> type) { Object value=get(key); return type.isInstance(value)?type.cast(value):null; }
    public void put(String key, Object value, Duration ttl) { try { redis.opsForValue().set(key, value, ttl); } catch (RuntimeException ignored) { } }
    public void evict(String key) { try { redis.delete(key); } catch (RuntimeException ignored) { } }
}


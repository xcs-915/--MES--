package com.tns.mes.common.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {
    private final RedisTemplate<String, Object> redis;
    @Value("${mes.idempotency.ttl-seconds:86400}") private long ttlSeconds;
    public IdempotencyService(@Qualifier("mesRedisTemplate") RedisTemplate<String, Object> redis) { this.redis=redis; }
    public boolean tryAcquire(String scope, String key) {
        if (key == null || key.trim().isEmpty()) return true;
        try { Boolean result=redis.opsForValue().setIfAbsent("mes:idempotency:"+scope+":"+key.trim(), "1", Duration.ofSeconds(ttlSeconds)); return Boolean.TRUE.equals(result); }
        catch (RuntimeException ex) { return true; }
    }
}


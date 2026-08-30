package com.tns.mes.common.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {
    private final RedisTemplate<String,Object> redis;
    @Value("${mes.rate-limit.requests-per-minute:120}") private long limit;
    public RateLimitService(@Qualifier("mesRedisTemplate") RedisTemplate<String,Object> redis){this.redis=redis;}
    public boolean allow(String key){try{String redisKey="mes:rate:"+key+":"+(System.currentTimeMillis()/60000);Long count=redis.opsForValue().increment(redisKey);if(count!=null&&count==1)redis.expire(redisKey, Duration.ofMinutes(2));return count==null||count<=limit;}catch(RuntimeException ex){return true;}}
}


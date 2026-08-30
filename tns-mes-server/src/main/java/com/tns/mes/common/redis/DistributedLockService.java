package com.tns.mes.common.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class DistributedLockService {
    private final RedisTemplate<String, Object> redis;
    public DistributedLockService(@Qualifier("mesRedisTemplate") RedisTemplate<String, Object> redis) { this.redis=redis; }
    public LockHandle tryLock(String key, Duration lease) {
        String value=UUID.randomUUID().toString();
        try { Boolean acquired=redis.opsForValue().setIfAbsent("mes:lock:"+key, value, lease); return Boolean.TRUE.equals(acquired)?new LockHandle("mes:lock:"+key,value):null; }
        catch (RuntimeException ex) { return null; }
    }
    public class LockHandle implements AutoCloseable {
        private final String key; private final String value; private boolean closed;
        LockHandle(String key,String value){this.key=key;this.value=value;}
        @Override public void close(){if(closed)return;closed=true;try{Object current=redis.opsForValue().get(key);if(value.equals(current))redis.delete(key);}catch(RuntimeException ignored){}}
    }
}


package com.tns.mes.common.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的分布式锁服务。
 *
 * <p>使用SET NX PX获取锁，释放时通过Lua脚本原子比较再删除，
 * 避免误删其他持有者的锁（双Pod场景下的核心安全问题）。</p>
 *
 * <p>当Redis不可用时，tryLock返回null（获取失败），调用方应降级处理。</p>
 */
@Service
public class DistributedLockService {

    private final RedisTemplate<String, Object> redis;

    /** Lua脚本: 仅当key的值等于传入value时才删除，保证原子释放 */
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end",
            Long.class
    );

    /** Lua脚本: 仅当key的值等于传入value时才续期，防止业务执行超时导致锁过期 */
    private static final RedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('pexpire', KEYS[1], ARGV[2]) " +
            "else " +
            "  return 0 " +
            "end",
            Long.class
    );

    public DistributedLockService(@Qualifier("mesRedisTemplate") RedisTemplate<String, Object> redis) {
        this.redis = redis;
    }

    /**
     * 尝试获取分布式锁。
     *
     * @param key  锁的业务键（不含前缀）
     * @param lease 持有时长
     * @return LockHandle（AutoCloseable），获取失败返回null
     */
    public LockHandle tryLock(String key, Duration lease) {
        String value = UUID.randomUUID().toString();
        String redisKey = "mes:lock:" + key;
        try {
            Boolean acquired = redis.opsForValue().setIfAbsent(redisKey, value, lease);
            if (Boolean.TRUE.equals(acquired)) {
                return new LockHandle(redisKey, value, lease);
            }
            return null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * 尝试获取锁，支持等待超时。
     *
     * @param key       锁的业务键
     * @param lease      持有时长
     * @param waitMillis 最大等待毫秒数
     * @return LockHandle，获取失败返回null
     */
    public LockHandle tryLock(String key, Duration lease, long waitMillis) {
        LockHandle handle = tryLock(key, lease);
        if (handle != null) return handle;

        long deadline = System.currentTimeMillis() + waitMillis;
        while (System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
            handle = tryLock(key, lease);
            if (handle != null) return handle;
        }
        return null;
    }

    /**
     * 锁句柄，实现AutoCloseable，可在try-with-resources中使用。
     * 释放时通过Lua脚本原子操作，确保只删除自己持有的锁。
     */
    public class LockHandle implements AutoCloseable {
        private final String key;
        private final String value;
        private final Duration lease;
        private volatile boolean closed = false;

        LockHandle(String key, String value, Duration lease) {
            this.key = key;
            this.value = value;
            this.lease = lease;
        }

        /**
         * 原子释放锁：仅当Redis中key的值等于本实例value时才删除。
         * 避免非原子操作（先get再delete）在双Pod并发下误删对方的锁。
         */
        @Override
        public void close() {
            if (closed) return;
            closed = true;
            try {
                Long result = redis.execute(UNLOCK_SCRIPT,
                        Collections.singletonList(key), value);
                // result==1 表示删除成功, result==0 表示锁已被其他持有者或已过期
            } catch (RuntimeException ignored) {
                // Redis不可用时静默释放，避免影响主链路
            }
        }

        /**
         * 续期锁（延长持有时长），防止长时间业务操作中锁提前过期。
         *
         * @param newLease 新的持有时长
         * @return true=续期成功, false=锁已不属于当前持有者
         */
        public boolean renew(Duration newLease) {
            if (closed) return false;
            try {
                Long result = redis.execute(RENEW_SCRIPT,
                        Collections.singletonList(key), value,
                        String.valueOf(newLease.toMillis()));
                return result != null && result > 0;
            } catch (RuntimeException ex) {
                return false;
            }
        }

        public boolean isClosed() {
            return closed;
        }
    }
}

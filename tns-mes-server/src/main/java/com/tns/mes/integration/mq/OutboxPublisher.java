package com.tns.mes.integration.mq;

import com.tns.mes.common.redis.DistributedLockService;
import com.tns.mes.integration.outbox.OutboxMessage;
import com.tns.mes.integration.outbox.OutboxMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Outbox 消息发布调度器。
 *
 * <p>定时轮询 PENDING 状态的 Outbox 消息，通过分布式锁保证双 Pod 环境下
 * 同一批消息不会被重复处理，发布到 RocketMQ 后更新状态。</p>
 *
 * <p>设计要点：
 * 1. 双 Pod 不重复抢占: 使用 Redis 分布式锁，只有一个 Pod 执行发布
 * 2. 可重试: 发布失败后递增 attempt_count，按指数退避设置 next_attempt_at
 * 3. 降级: RocketMQ 不可用时消息保留 PENDING，不影响主业务
 * 4. 死信: 超过 maxAttempts 后标记为 FAILED，需人工介入</p>
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final String LOCK_KEY = "outbox:publish";
    private static final Duration LOCK_LEASE = Duration.ofSeconds(30);
    private static final long LOCK_WAIT_MILLIS = Duration.ofSeconds(2).toMillis();

    private final OutboxMessageRepository outboxRepository;
    private final MessagePublisher messagePublisher;
    private final RocketMQProperties properties;
    private final DistributedLockService lockService;

    @Autowired
    public OutboxPublisher(OutboxMessageRepository outboxRepository,
                           MessagePublisher messagePublisher,
                           RocketMQProperties properties,
                           DistributedLockService lockService) {
        this.outboxRepository = outboxRepository;
        this.messagePublisher = messagePublisher;
        this.properties = properties;
        this.lockService = lockService;
    }

    /**
     * 定时轮询发布 Outbox 消息。
     * fixedDelayString 支持通过配置 mes.integration.rocketmq.poll-interval-ms 调整。
     * initialDelay 10秒，等待应用完全启动。
     */
    @Scheduled(fixedDelayString = "${mes.integration.rocketmq.poll-interval-ms:5000}",
               initialDelayString = "10000")
    public void pollAndPublish() {
        // RocketMQ 不可用时直接跳过，不持有锁也不查库
        if (!messagePublisher.isAvailable()) {
            return;
        }

        // 分布式锁：双 Pod 下只有一个 Pod 执行发布
        try (DistributedLockService.LockHandle lock =
                     lockService.tryLock(LOCK_KEY, LOCK_LEASE, LOCK_WAIT_MILLIS)) {
            if (lock == null) {
                // 未获取到锁，说明另一 Pod 正在处理，本轮跳过
                return;
            }
            processBatch();
        }
    }

    /**
     * 处理一批待发布的 Outbox 消息。
     *
     * <p>双 Pod 不重复抢占的保障层次：
     * 1. Redis 分布式锁（外层）：只有一个 Pod 进入 processBatch
     * 2. SQL Server UPDLOCK+READPAST（内层）：即使 Redis 锁失效，
     *    DB 行级锁也能确保同一批消息不会被两个事务同时读取</p>
     */
    @Transactional
    public void processBatch() {
        Instant now = Instant.now();
        // 使用 SQL Server 原子抢占查询（UPDLOCK, READPAST）
        List<OutboxMessage> messages = outboxRepository
                .claimPendingForUpdate(now, properties.getBatchSize());

        if (messages.isEmpty()) {
            return;
        }

        log.debug("Processing {} outbox messages", messages.size());

        for (OutboxMessage msg : messages) {
            try {
                boolean success = messagePublisher.publish(
                        properties.getTopic(),
                        msg.getEventType(),
                        msg.getEventId(),
                        msg.getPayload()
                );

                if (success) {
                    msg.setStatus("PUBLISHED");
                    msg.setPublishedAt(Instant.now());
                    msg.setUpdatedAt(Instant.now());
                    outboxRepository.save(msg);
                } else {
                    handleFailure(msg, "Publisher returned false (RocketMQ unavailable or send failed)");
                }
            } catch (Exception e) {
                handleFailure(msg, e.getMessage());
            }
        }
    }

    /**
     * 处理发布失败：递增重试次数，指数退避设置下次重试时间。
     * 超过最大重试次数后标记为 FAILED。
     */
    private void handleFailure(OutboxMessage msg, String error) {
        int attempts = msg.getAttemptCount() + 1;
        msg.setAttemptCount(attempts);
        msg.setLastError(error != null && error.length() > 1900
                ? error.substring(0, 1900) : error);
        msg.setUpdatedAt(Instant.now());

        if (attempts >= properties.getMaxAttempts()) {
            // 超过最大重试次数，标记为失败（死信）
            msg.setStatus("FAILED");
            log.warn("Outbox message marked as FAILED after {} attempts: eventId={}",
                    attempts, msg.getEventId());
        } else {
            // 指数退避: 2^attempts 秒 (2s, 4s, 8s, 16s, 32s...)
            long backoffSeconds = (long) Math.pow(2, attempts);
            msg.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds));
        }
        outboxRepository.save(msg);
    }
}

package com.tns.mes.integration.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox 消息服务 —— 事务性消息发件箱入口。
 *
 * <p>核心流程：
 * 1. 业务事务中调用 enqueue() 将事件写入 mes_outbox_message 表
 * 2. 事务提交后，OutboxPublisher 定时轮询并发布到 RocketMQ
 * 3. 发布成功 → PUBLISHED；发布失败 → 指数退避重试；超过上限 → FAILED
 *
 * <p>设计原则：消息写入与业务数据变更在同一数据库事务中，
 * 保证 "要么都成功，要么都回滚" 的最终一致性。</p>
 */
@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxMessageRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxMessageRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * 将业务事件入队（在调用方的数据库事务中执行）。
     *
     * @param aggregateType 聚合类型（如 "WorkOrder", "Product"）
     * @param aggregateId   聚合ID
     * @param eventType     事件类型（如 "work_order.created"）
     * @param payload       事件负载对象（将被序列化为JSON）
     * @return 持久化后的 OutboxMessage
     */
    @Transactional
    public OutboxMessage enqueue(String aggregateType, String aggregateId,
                                  String eventType, Object payload) {
        OutboxMessage message = new OutboxMessage();
        message.setEventId(UUID.randomUUID().toString().replace("-", ""));
        message.setAggregateType(aggregateType);
        message.setAggregateId(aggregateId);
        message.setEventType(eventType);
        try {
            message.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to serialize outbox payload", ex);
        }
        message.setNextAttemptAt(Instant.now());
        return repository.save(message);
    }

    /**
     * 延迟入队：设置 next_attempt_at 为未来时间，用于定时触发场景。
     */
    @Transactional
    public OutboxMessage enqueueDelayed(String aggregateType, String aggregateId,
                                         String eventType, Object payload,
                                         Instant scheduledAt) {
        OutboxMessage message = enqueue(aggregateType, aggregateId, eventType, payload);
        message.setNextAttemptAt(scheduledAt);
        return repository.save(message);
    }

    /**
     * 标记消息为已发布（OutboxPublisher 调用）。
     */
    @Transactional
    public void markPublished(Long id) {
        repository.findById(id).ifPresent(msg -> {
            msg.setStatus("PUBLISHED");
            msg.setPublishedAt(Instant.now());
            msg.setUpdatedAt(Instant.now());
            repository.save(msg);
        });
    }

    /**
     * 标记消息为失败（超过最大重试次数时调用）。
     */
    @Transactional
    public void markFailed(Long id, String error) {
        repository.findById(id).ifPresent(msg -> {
            msg.setStatus("FAILED");
            msg.setLastError(error != null && error.length() > 1900
                    ? error.substring(0, 1900) : error);
            msg.setUpdatedAt(Instant.now());
            repository.save(msg);
            log.warn("Outbox message FAILED: id={}, eventId={}, error={}",
                    id, msg.getEventId(), error);
        });
    }

    /**
     * 重置 FAILED 消息为 PENDING（人工干预后重新触发）。
     */
    @Transactional
    public void retryFailed(Long id) {
        repository.findById(id).ifPresent(msg -> {
            msg.setStatus("PENDING");
            msg.setAttemptCount(0);
            msg.setNextAttemptAt(Instant.now());
            msg.setLastError(null);
            msg.setUpdatedAt(Instant.now());
            repository.save(msg);
            log.info("Outbox message retried: id={}, eventId={}", id, msg.getEventId());
        });
    }
}

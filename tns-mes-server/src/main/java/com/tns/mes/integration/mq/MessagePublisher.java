package com.tns.mes.integration.mq;

/**
 * 消息发布接口。
 *
 * <p>实现策略：
 * - RocketMQMessagePublisher: 当 mes.integration.rocketmq.enabled=true 时激活
 * - NoopMessagePublisher: 当 RocketMQ 未启用或不可用时的降级实现</p>
 *
 * <p>降级原则：发布失败不影响主业务事务，Outbox 消息会在后续轮询中重试。</p>
 */
public interface MessagePublisher {

    /**
     * 发布消息到消息中间件。
     *
     * @param topic     主题
     * @param tags      标签（可用于事件类型路由）
     * @param key       消息业务键（用于幂等去重）
     * @param body      消息体（JSON字符串）
     * @return true=发布成功, false=发布失败（将重试）
     */
    boolean publish(String topic, String tags, String key, String body);

    /**
     * 是否处于活跃状态（中间件已连接且可发布）。
     */
    boolean isAvailable();
}

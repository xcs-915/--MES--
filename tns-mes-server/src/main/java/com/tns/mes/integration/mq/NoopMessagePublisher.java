package com.tns.mes.integration.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 空操作消息发布器 —— 降级实现。
 *
 * <p>当 RocketMQ 未启用（mes.integration.rocketmq.enabled=false）时使用。
 * 所有 publish 调用直接返回 false，OutboxPublisher 会保留消息为 PENDING 状态，
 * 待 RocketMQ 启用后再行发布。这样保证业务事务不受影响。</p>
 *
 * <p>通过 {@link MessagePublisherAutoConfiguration} 的 @Bean 方法创建，
 * 不使用 @Component，因为 @ConditionalOnMissingBean 在 @Component 上不可靠。</p>
 */
public class NoopMessagePublisher implements MessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(NoopMessagePublisher.class);

    @Override
    public boolean publish(String topic, String tags, String key, String body) {
        log.debug("RocketMQ disabled, message skipped: topic={}, key={}", topic, key);
        return false;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}

package com.tns.mes.integration.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消息发布器自动配置。
 *
 * <p>根据功能开关 mes.integration.rocketmq.enabled 选择实现：
 * <ul>
 *   <li>true: 创建 RocketMQMessagePublisher，连接 RocketMQ 发布事件</li>
 *   <li>false: 创建 NoopMessagePublisher，消息仅存 Outbox 不发布</li>
 * </ul>
 *
 * <p>使用 @Configuration + @Bean 方式（而非 @Component + @ConditionalOnMissingBean），
 * 因为 @ConditionalOnMissingBean 在 @Component 类上不可靠，
 * 在组件扫描顺序不确定时可能导致 bean 缺失。</p>
 */
@Configuration
public class MessagePublisherAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MessagePublisherAutoConfiguration.class);

    /**
     * RocketMQ 实现：仅当功能开关启用时创建。
     * 如果 Producer 初始化失败，内部 isAvailable() 返回 false，自动降级。
     */
    @Bean
    @ConditionalOnProperty(name = "mes.integration.rocketmq.enabled", havingValue = "true")
    public MessagePublisher rocketMQMessagePublisher(RocketMQProperties properties) {
        log.info("Creating RocketMQMessagePublisher (mes.integration.rocketmq.enabled=true)");
        return new RocketMQMessagePublisher(properties);
    }

    /**
     * 降级实现：当 RocketMQ 未启用或不可用时使用。
     * 所有 publish 调用返回 false，Outbox 消息保留 PENDING 等待后续重试。
     */
    @Bean
    @ConditionalOnMissingBean(MessagePublisher.class)
    public MessagePublisher noopMessagePublisher() {
        log.info("Creating NoopMessagePublisher (RocketMQ disabled or unavailable)");
        return new NoopMessagePublisher();
    }
}

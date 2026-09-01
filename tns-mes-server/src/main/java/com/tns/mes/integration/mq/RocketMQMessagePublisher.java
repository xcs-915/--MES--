package com.tns.mes.integration.mq;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

/**
 * RocketMQ 消息发布器实现。
 *
 * <p>仅当 mes.integration.rocketmq.enabled=true 时由
 * {@link MessagePublisherAutoConfiguration} 创建。
 * 启动时初始化 DefaultMQProducer，关闭时优雅释放资源。</p>
 *
 * <p>降级策略：
 * - Producer 初始化失败时记录错误，isAvailable()返回false
 * - 单条消息发送失败返回false，不影响其他消息
 * - 所有异常被捕获，不会抛出影响主链路</p>
 */
public class RocketMQMessagePublisher implements MessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(RocketMQMessagePublisher.class);

    private final RocketMQProperties properties;
    private DefaultMQProducer producer;
    private volatile boolean available = false;

    public RocketMQMessagePublisher(RocketMQProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        try {
            producer = new DefaultMQProducer(properties.getProducerGroup());
            producer.setNamesrvAddr(properties.getNameServer());
            producer.setSendMsgTimeout(properties.getSendTimeoutMs());
            producer.setRetryTimesWhenSendFailed(properties.getRetryTimes());
            producer.start();
            available = true;
            log.info("RocketMQ producer started: nameserver={}, group={}",
                    properties.getNameServer(), properties.getProducerGroup());
        } catch (MQClientException e) {
            log.error("RocketMQ producer init failed, falling back to no-op mode: {}", e.getMessage());
            available = false;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (producer != null) {
            try {
                producer.shutdown();
                log.info("RocketMQ producer shut down gracefully");
            } catch (RuntimeException e) {
                log.warn("RocketMQ producer shutdown error: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean publish(String topic, String tags, String key, String body) {
        if (!available || producer == null) {
            return false;
        }
        try {
            Message msg = new Message(
                    topic != null ? topic : properties.getTopic(),
                    tags,
                    key,
                    body.getBytes(StandardCharsets.UTF_8)
            );
            SendResult result = producer.send(msg);
            return result != null && SendStatus.SEND_OK.equals(result.getSendStatus());
        } catch (Exception e) {
            log.warn("RocketMQ publish failed, will retry later: topic={}, key={}, error={}",
                    topic, key, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}

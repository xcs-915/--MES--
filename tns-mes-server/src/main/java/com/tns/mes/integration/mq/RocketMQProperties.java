package com.tns.mes.integration.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 配置属性。
 *
 * <p>通过 mes.integration.rocketmq.enabled 功能开关控制：
 * - true:  创建 DefaultMQProducer Bean，Outbox 发布链路激活
 * - false: 使用 NoopMessagePublisher，Outbox 消息仅存库不发布</p>
 */
@Component
@ConfigurationProperties(prefix = "mes.integration.rocketmq")
public class RocketMQProperties {

    /** 功能开关：是否启用 RocketMQ 消息发布 */
    private boolean enabled = false;

    /** NameServer 地址 */
    private String nameServer;

    /** Producer Group 名称 */
    private String producerGroup = "tns-mes-producer";

    /** 发布主题 */
    private String topic = "tns-mes-events";

    /** 发送超时（毫秒） */
    private int sendTimeoutMs = 3000;

    /** 发送失败重试次数 */
    private int retryTimes = 2;

    /** Outbox 轮询间隔（毫秒） */
    private long pollIntervalMs = 5000;

    /** 单次轮询最大处理消息数 */
    private int batchSize = 50;

    /** 最大重试次数（超过后标记为FAILED） */
    private int maxAttempts = 5;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getNameServer() { return nameServer; }
    public void setNameServer(String nameServer) { this.nameServer = nameServer; }

    public String getProducerGroup() { return producerGroup; }
    public void setProducerGroup(String producerGroup) { this.producerGroup = producerGroup; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public int getSendTimeoutMs() { return sendTimeoutMs; }
    public void setSendTimeoutMs(int sendTimeoutMs) { this.sendTimeoutMs = sendTimeoutMs; }

    public int getRetryTimes() { return retryTimes; }
    public void setRetryTimes(int retryTimes) { this.retryTimes = retryTimes; }

    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
}

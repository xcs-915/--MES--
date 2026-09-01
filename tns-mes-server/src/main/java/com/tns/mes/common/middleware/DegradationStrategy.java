package com.tns.mes.common.middleware;

import com.tns.mes.integration.mq.MessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * 中间件降级策略 —— 集中管理降级决策。
 *
 * <p>业务代码不直接读取功能开关或探测 Bean 状态，而是统一询问本服务，从而将
 * 降级语义收敛在一处，便于后续演进、测试与监控。</p>
 *
 * <p>提供三个核心方法：
 * <ul>
 *   <li>{@link #isMiddlewareAvailable(String)}：中间件是否既已启用、其 Bean 又可用。</li>
 *   <li>{@link #getDegradationMessage(String)}：当前生效的降级说明（人类可读）。</li>
 *   <li>{@link #shouldUseFallback(String)}：调用方是否应改走兜底逻辑（禁用或不可用即返回 true）。</li>
 * </ul>
 *
 * <p>判断依据与 {@link MiddlewareHealthIndicator} 完全一致：功能开关 + {@link ObjectProvider}
 * 探测的 Bean 可用性，因此两处结论不会相互矛盾。</p>
 */
@Service
public class DegradationStrategy {

    private static final Logger log = LoggerFactory.getLogger(DegradationStrategy.class);

    private static final String REDIS_HEALTH_KEY = "mes:health-check";

    private final boolean rocketmqEnabled;
    private final boolean sentinelEnabled;
    private final boolean nacosEnabled;
    private final boolean seataEnabled;

    private final ObjectProvider<MessagePublisher> messagePublisherProvider;
    private final ObjectProvider<NacosAutoConfiguration> nacosAutoConfigurationProvider;
    private final ObjectProvider<SeataAutoConfiguration> seataAutoConfigurationProvider;
    private final ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider;

    public DegradationStrategy(
            @Value("${mes.integration.rocketmq.enabled:false}") boolean rocketmqEnabled,
            @Value("${mes.integration.sentinel.enabled:false}") boolean sentinelEnabled,
            @Value("${mes.integration.nacos.enabled:false}") boolean nacosEnabled,
            @Value("${mes.integration.seata.enabled:false}") boolean seataEnabled,
            ObjectProvider<MessagePublisher> messagePublisherProvider,
            ObjectProvider<NacosAutoConfiguration> nacosAutoConfigurationProvider,
            ObjectProvider<SeataAutoConfiguration> seataAutoConfigurationProvider,
            ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) {
        this.rocketmqEnabled = rocketmqEnabled;
        this.sentinelEnabled = sentinelEnabled;
        this.nacosEnabled = nacosEnabled;
        this.seataEnabled = seataEnabled;
        this.messagePublisherProvider = messagePublisherProvider;
        this.nacosAutoConfigurationProvider = nacosAutoConfigurationProvider;
        this.seataAutoConfigurationProvider = seataAutoConfigurationProvider;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    /**
     * 中间件是否既已启用又可用。
     *
     * @param name 中间件名称（rocketmq / sentinel / nacos / seata / redis，大小写不敏感）
     * @return true 表示该中间件处于可用状态；未知名称返回 false 并记录警告
     */
    public boolean isMiddlewareAvailable(String name) {
        String n = normalize(name);
        switch (n) {
            case "rocketmq":
                return rocketmqEnabled && mqAvailable();
            case "sentinel":
                // Sentinel 无独立 Bean 可探测，启用即视为可用
                return sentinelEnabled;
            case "nacos":
                return nacosEnabled && nacosAvailable();
            case "seata":
                return seataEnabled && seataAvailable();
            case "redis":
                return redisAvailable();
            default:
                log.warn("Unknown middleware name '{}', treating as unavailable", name);
                return false;
        }
    }

    /**
     * 返回当前生效的降级说明（人类可读）。
     *
     * @param middlewareName 中间件名称（大小写不敏感）
     * @return 降级说明文本；若该中间件正常则说明无降级生效
     */
    public String getDegradationMessage(String middlewareName) {
        String n = normalize(middlewareName);
        switch (n) {
            case "rocketmq":
                if (!rocketmqEnabled) {
                    return "RocketMQ is disabled; events are persisted in the outbox and will be retried once enabled.";
                }
                if (!mqAvailable()) {
                    return "RocketMQ is enabled but currently unavailable; events remain in the outbox and will be retried.";
                }
                return "RocketMQ is operating normally; no degradation in effect.";
            case "sentinel":
                if (!sentinelEnabled) {
                    return "Sentinel is disabled; flow control and circuit breaking fall back to local defaults.";
                }
                return "Sentinel is operating normally; no degradation in effect.";
            case "nacos":
                if (!nacosEnabled) {
                    return "Nacos is disabled; configuration and service discovery use local static values.";
                }
                if (!nacosAvailable()) {
                    return "Nacos is enabled but currently unavailable; falling back to local configuration.";
                }
                return "Nacos is operating normally; no degradation in effect.";
            case "seata":
                if (!seataEnabled) {
                    return "Seata is disabled; distributed transactions run in local best-effort mode.";
                }
                if (!seataAvailable()) {
                    return "Seata is enabled but currently unavailable; distributed transactions fall back to local mode.";
                }
                return "Seata is operating normally; no degradation in effect.";
            case "redis":
                if (!redisAvailable()) {
                    return "Redis is unavailable; caching, locking and idempotency fall back to local best-effort behavior.";
                }
                return "Redis is operating normally; no degradation in effect.";
            default:
                return "Unknown middleware '" + middlewareName + "'; no degradation information available.";
        }
    }

    /**
     * 调用方是否应改走兜底逻辑。
     *
     * <p>当中间件被禁用或不可用时返回 true，表示业务应使用本地/降级实现。
     * 等价于 {@code !isMiddlewareAvailable(middlewareName)}。</p>
     *
     * @param middlewareName 中间件名称（大小写不敏感）
     * @return true 表示应使用兜底逻辑；未知名称也返回 true（保守降级）
     */
    public boolean shouldUseFallback(String middlewareName) {
        return !isMiddlewareAvailable(middlewareName);
    }

    private boolean mqAvailable() {
        MessagePublisher publisher = messagePublisherProvider.getIfAvailable();
        return publisher != null && publisher.isAvailable();
    }

    private boolean nacosAvailable() {
        NacosAutoConfiguration nacos = nacosAutoConfigurationProvider.getIfAvailable();
        return nacos != null && nacos.isAvailable();
    }

    private boolean seataAvailable() {
        SeataAutoConfiguration seata = seataAutoConfigurationProvider.getIfAvailable();
        return seata != null && seata.isAvailable();
    }

    private boolean redisAvailable() {
        RedisTemplate<String, Object> redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return false;
        }
        try {
            redis.opsForValue().get(REDIS_HEALTH_KEY);
            return true;
        } catch (Exception e) {
            log.debug("Redis availability check failed: {}", e.getMessage());
            return false;
        }
    }

    private String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}

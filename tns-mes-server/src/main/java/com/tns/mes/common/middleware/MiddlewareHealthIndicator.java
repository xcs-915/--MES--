package com.tns.mes.common.middleware;

import com.tns.mes.integration.mq.MessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 中间件健康指标。
 *
 * <p>聚合所有可选中间件（RocketMQ、Sentinel、Nacos、Seata、Redis）的启用与可用状态，
 * 暴露在 Actuator {@code /health} 端点的 {@code middleware} 子节点下。</p>
 *
 * <p>设计要点：
 * <ul>
 *   <li>所有可选 Bean 通过 {@link ObjectProvider} 注入，中间件未启用或缺失时不会导致应用启动失败。</li>
 *   <li>仅当某中间件被功能开关启用（enabled=true）但其 Bean 不可用时，整体状态置为 DOWN。</li>
 *   <li>被禁用的中间件以 {@code enabled:false, status:"disabled"} 形式展示，不影响整体状态。</li>
 *   <li>Redis 没有独立的 {@code mes.integration.*.enabled} 开关，其 {@code enabled} 取决于
 *       {@code RedisTemplate} Bean 是否存在；可用性通过 {@code redis.opsForValue().get("mes:health-check")}
 *       探测。为避免本地开发环境（无 Redis）误报 DOWN，Redis 仅作为明细展示，不强制整体 DOWN；
 *       生产环境 Redis 连通性由 Spring 内置的 {@code RedisHealthIndicator} 负责。</li>
 * </ul>
 *
 * <p>降级原则与本项目的 {@code NoopMessagePublisher} 一致：中间件不可用不影响主业务事务。</p>
 */
@Component
public class MiddlewareHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(MiddlewareHealthIndicator.class);

    /** Redis 连通性探测使用的键，不要求该键真实存在，仅需 GET 不抛异常。 */
    private static final String REDIS_HEALTH_KEY = "mes:health-check";

    private final boolean rocketmqEnabled;
    private final boolean sentinelEnabled;
    private final boolean nacosEnabled;
    private final boolean seataEnabled;

    private final ObjectProvider<MessagePublisher> messagePublisherProvider;
    private final ObjectProvider<NacosAutoConfiguration> nacosAutoConfigurationProvider;
    private final ObjectProvider<SeataAutoConfiguration> seataAutoConfigurationProvider;
    private final ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider;

    public MiddlewareHealthIndicator(
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

    @PostConstruct
    public void init() {
        log.info("MiddlewareHealthIndicator registered: rocketmq={}, sentinel={}, nacos={}, seata={}",
                rocketmqEnabled, sentinelEnabled, nacosEnabled, seataEnabled);
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        boolean allEnabledAvailable = true;

        // RocketMQ：NoopMessagePublisher 始终存在，isAvailable() 区分真实可用与降级
        if (rocketmqEnabled) {
            MessagePublisher publisher = messagePublisherProvider.getIfAvailable();
            boolean available = publisher != null && publisher.isAvailable();
            details.put("rocketmq", enabledDetail(available));
            if (!available) {
                allEnabledAvailable = false;
            }
        } else {
            details.put("rocketmq", disabledDetail());
        }

        // Sentinel：无独立 Bean 可探测，启用即视为可用
        if (sentinelEnabled) {
            details.put("sentinel", enabledDetail(true));
        } else {
            details.put("sentinel", disabledDetail());
        }

        // Nacos
        if (nacosEnabled) {
            NacosAutoConfiguration nacos = nacosAutoConfigurationProvider.getIfAvailable();
            boolean available = nacos != null && nacos.isAvailable();
            details.put("nacos", enabledDetail(available));
            if (!available) {
                allEnabledAvailable = false;
            }
        } else {
            details.put("nacos", disabledDetail());
        }

        // Seata
        if (seataEnabled) {
            SeataAutoConfiguration seata = seataAutoConfigurationProvider.getIfAvailable();
            boolean available = seata != null && seata.isAvailable();
            details.put("seata", enabledDetail(available));
            if (!available) {
                allEnabledAvailable = false;
            }
        } else {
            details.put("seata", disabledDetail());
        }

        // Redis：无 mes 开关，enabled = RedisTemplate 是否存在；仅展示，不强制 DOWN
        RedisTemplate<String, Object> redis = redisTemplateProvider.getIfAvailable();
        if (redis != null) {
            details.put("redis", enabledDetail(checkRedis(redis)));
        } else {
            details.put("redis", disabledDetail());
        }

        Health.Builder builder = allEnabledAvailable ? Health.up() : Health.down();
        details.forEach(builder::withDetail);
        return builder.build();
    }

    /**
     * 通过一次 GET 探测 Redis 连通性。键无需真实存在，只要不抛异常即视为可用。
     */
    private boolean checkRedis(RedisTemplate<String, Object> redis) {
        try {
            redis.opsForValue().get(REDIS_HEALTH_KEY);
            return true;
        } catch (Exception e) {
            log.warn("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }

    private Map<String, Object> enabledDetail(boolean available) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("enabled", true);
        detail.put("available", available);
        return detail;
    }

    private Map<String, Object> disabledDetail() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("enabled", false);
        detail.put("status", "disabled");
        return detail;
    }
}

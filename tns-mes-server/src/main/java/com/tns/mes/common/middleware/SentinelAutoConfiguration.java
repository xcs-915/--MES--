package com.tns.mes.common.middleware;

import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import com.alibaba.csp.sentinel.transport.heartbeat.SimpleHttpHeartbeatSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;

/**
 * Sentinel 流控自动配置。
 *
 * <p>采用"功能开关 + 默认兼容单体"模式：
 * <ul>
 *   <li>mes.integration.sentinel.enabled=true: 初始化 Sentinel 传输层，
 *       {@link SentinelFlowControlFilter} 包装 API 入口实现限流</li>
 *   <li>mes.integration.sentinel.enabled=false: 不创建此配置类和 Filter，
 *       应用以单体模式运行，无流控开销</li>
 * </ul>
 *
 * <p>降级策略：初始化失败时记录错误日志，不中断应用启动。
 * Sentinel 在不可用时不影响业务请求的正常处理。</p>
 *
 * <p>依赖 sentinel-core 1.8.6 + sentinel-transport-simple-http 1.8.6。
 * Dashboard 地址通过 mes.integration.sentinel.dashboard 配置，
 * 对应 Sentinel 内部属性 csp.sentinel.dashboard.server。</p>
 *
 * @see SentinelFlowControlFilter
 */
@Configuration
@ConditionalOnProperty(name = "mes.integration.sentinel.enabled", havingValue = "true")
public class SentinelAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SentinelAutoConfiguration.class);

    /** 默认流控资源名，与 Filter 中 SphU.entry 的资源名保持一致 */
    private static final String RESOURCE_NAME = "mes-api";

    /** 默认 QPS 限流阈值 */
    private static final int DEFAULT_QPS_LIMIT = 100;

    /** 应用名，用于 Sentinel Dashboard 中的客户端标识 */
    private static final String APP_NAME = "tns-mes-server";

    @Value("${mes.integration.sentinel.dashboard:}")
    private String dashboard;

    /** 心跳发送器实例，生命周期由本类管理 */
    private SimpleHttpHeartbeatSender heartbeatSender;

    /**
     * 初始化 Sentinel 传输层和默认流控规则。
     *
     * <p>执行步骤：
     * <ol>
     *   <li>设置应用名（project.name 系统属性），必须在 SentinelConfig 类加载前设置，
     *       SentinelConfigLoader 静态初始化时会读取系统属性</li>
     *   <li>通过 SentinelConfig 设置 Dashboard 地址（csp.sentinel.dashboard.server）</li>
     *   <li>加载默认流控规则：资源 mes-api，QPS 阈值 100</li>
     *   <li>创建 SimpleHttpHeartbeatSender 并发送初始心跳</li>
     * </ol>
     *
     * <p>注：SentinelConfig.PROJECT_NAME_PROP_KEY 为编译期常量，访问它不会触发类加载。
     * SentinelConfig.setConfig() 是首次触发 SentinelConfig 类初始化的调用点，
     * 此时 SentinelConfigLoader 会读取已设置的系统属性完成 appName 解析。</p>
     *
     * <p>注：Sentinel 的命令中心（嵌入式 HTTP 服务）和定时心跳调度由
     * Sentinel SPI 初始化框架在首次 SphU.entry 调用时自动启动，
     * 此处的初始心跳用于验证 Dashboard 连通性。</p>
     */
    @PostConstruct
    public void init() {
        try {
            // 设置应用名（系统属性），必须在 SentinelConfig 类加载前完成。
            // SentinelConfig.PROJECT_NAME_PROP_KEY 是 static final String 编译期常量，
            // 访问它不会触发 SentinelConfig 类初始化。
            if (System.getProperty(SentinelConfig.PROJECT_NAME_PROP_KEY) == null) {
                System.setProperty(SentinelConfig.PROJECT_NAME_PROP_KEY, APP_NAME);
                log.info("Sentinel app name set via system property: {}", APP_NAME);
            }

            // 配置 Dashboard 地址（此处首次触发 SentinelConfig 类加载）
            if (dashboard != null && !dashboard.isEmpty()) {
                SentinelConfig.setConfig(TransportConfig.CONSOLE_SERVER, dashboard);
                log.info("Sentinel dashboard configured: {}", dashboard);
            } else {
                log.warn("Sentinel dashboard address not configured, heartbeat will not be sent");
            }

            // 加载默认流控规则：mes-api QPS 限流 100
            FlowRule rule = new FlowRule();
            rule.setResource(RESOURCE_NAME);
            rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
            rule.setCount(DEFAULT_QPS_LIMIT);
            FlowRuleManager.loadRules(Collections.singletonList(rule));
            log.info("Sentinel flow rule loaded: resource={}, grade=QPS, limit={}",
                    RESOURCE_NAME, DEFAULT_QPS_LIMIT);

            // 初始化心跳发送器并发送初始心跳
            heartbeatSender = new SimpleHttpHeartbeatSender();
            boolean sent = heartbeatSender.sendHeartbeat();
            if (sent) {
                log.info("Sentinel transport initialized, initial heartbeat sent successfully");
            } else {
                log.info("Sentinel transport initialized, initial heartbeat not sent " +
                        "(command center may not be started yet, will start on first request)");
            }
        } catch (Exception e) {
            log.error("Sentinel initialization failed, continuing without Sentinel flow control: {}",
                    e.getMessage(), e);
        }
    }

    /**
     * 关闭时清理 Sentinel 资源。
     *
     * <p>清除已加载的流控规则，释放心跳发送器引用。
     * Sentinel 的定时心跳调度线程池由 SPI 初始化框架管理，
     * 随 JVM 关闭自动终止。</p>
     */
    @PreDestroy
    public void destroy() {
        try {
            FlowRuleManager.loadRules(Collections.emptyList());
            log.info("Sentinel flow rules cleared on shutdown");
        } catch (Exception e) {
            log.warn("Failed to clear Sentinel flow rules on shutdown: {}", e.getMessage());
        }
        heartbeatSender = null;
        log.info("Sentinel heartbeat sender released");
    }
}

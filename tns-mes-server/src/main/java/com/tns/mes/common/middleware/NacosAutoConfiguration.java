package com.tns.mes.common.middleware;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Nacos 服务注册与配置中心自动配置。
 *
 * <p>采用"功能开关 + 默认兼容单体"模式：
 * <ul>
 *   <li>mes.integration.nacos.enabled=true: 创建 NamingService 注册实例，
 *       创建 ConfigService 监听外部化配置变更</li>
 *   <li>mes.integration.nacos.enabled=false: 不创建此配置类，
 *       应用使用本地静态配置运行</li>
 * </ul>
 *
 * <p>降级策略：Nacos 初始化失败时记录错误日志，isAvailable()返回false，
 * 不中断应用启动。中间件不可用时 DegradationStrategy 自动降级到本地配置。</p>
 *
 * <p>依赖 nacos-client 2.2.3。地址通过 mes.integration.nacos.addr 配置。</p>
 */
@Configuration
@ConditionalOnProperty(name = "mes.integration.nacos.enabled", havingValue = "true")
public class NacosAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(NacosAutoConfiguration.class);

    @Value("${mes.integration.nacos.addr:}")
    private String addr;

    @Value("${spring.application.name:tns-mes-server}")
    private String serviceName;

    @Value("${server.port:8080}")
    private int port;

    @Value("${mes.integration.nacos.username:}")
    private String username;

    @Value("${mes.integration.nacos.password:}")
    private String password;

    private volatile boolean available = false;
    private NamingService namingService;
    private ConfigService configService;
    private String registeredIp;

    @PostConstruct
    public void init() {
        try {
            // 解析本地IP
            registeredIp = resolveLocalIp();
            log.info("Nacos init: addr={}, service={}, ip={}, port={}", addr, serviceName, registeredIp, port);

            // 创建 NamingService 并注册实例
            Properties namingProps = new Properties();
            namingProps.setProperty("serverAddr", addr);
            if (username != null && !username.trim().isEmpty()) namingProps.setProperty("username", username.trim());
            if (password != null && !password.trim().isEmpty()) namingProps.setProperty("password", password);
            namingService = NacosFactory.createNamingService(namingProps);
            Instance instance = new Instance();
            instance.setIp(registeredIp);
            instance.setPort(port);
            instance.setWeight(1.0);
            instance.setHealthy(true);
            namingService.registerInstance(serviceName, instance);
            log.info("Nacos service registered: {} @ {}:{}", serviceName, registeredIp, port);

            // 创建 ConfigService 并监听配置变更
            Properties configProps = new Properties();
            configProps.setProperty("serverAddr", addr);
            if (username != null && !username.trim().isEmpty()) configProps.setProperty("username", username.trim());
            if (password != null && !password.trim().isEmpty()) configProps.setProperty("password", password);
            configService = NacosFactory.createConfigService(configProps);
            String dataId = serviceName + ".yaml";
            String group = "DEFAULT_GROUP";

            // 拉取初始配置
            String currentConfig = configService.getConfig(dataId, group, 3000);
            if (currentConfig != null && !currentConfig.isEmpty()) {
                log.info("Nacos config loaded: dataId={}, group={}", dataId, group);
            }

            // 添加配置监听
            configService.addListener(dataId, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null; // 使用默认线程
                }
                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("Nacos config changed: dataId={}, newLength={}", dataId,
                            configInfo != null ? configInfo.length() : 0);
                }
            });
            log.info("Nacos config listener added: dataId={}", dataId);

            available = true;
            log.info("Nacos integration initialized successfully");
        } catch (NacosException e) {
            log.error("Nacos init failed (NacosException), continuing without Nacos: errorCode={}, msg={}",
                    e.getErrCode(), e.getMessage());
            available = false;
        } catch (Exception e) {
            log.error("Nacos init failed, continuing without Nacos: {}", e.getMessage(), e);
            available = false;
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (namingService != null && registeredIp != null) {
                Instance instance = new Instance();
                instance.setIp(registeredIp);
                instance.setPort(port);
                namingService.deregisterInstance(serviceName, instance);
                log.info("Nacos service deregistered: {}", serviceName);
            }
        } catch (Exception e) {
            log.warn("Nacos deregister error: {}", e.getMessage());
        }
        try {
            if (configService != null) {
                configService.shutDown();
                log.info("Nacos config service shut down");
            }
        } catch (Exception e) {
            log.warn("Nacos config service shutdown error: {}", e.getMessage());
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * 从 Nacos 获取配置值。
     *
     * @param dataId       配置ID
     * @param defaultValue 默认值（Nacos 不可用或配置不存在时返回）
     * @return 配置值
     */
    public String getConfigValue(String dataId, String defaultValue) {
        if (!available || configService == null) {
            return defaultValue;
        }
        try {
            String value = configService.getConfig(dataId, "DEFAULT_GROUP", 3000);
            return value != null && !value.isEmpty() ? value : defaultValue;
        } catch (NacosException e) {
            log.warn("Nacos getConfig failed: dataId={}, error={}", dataId, e.getMessage());
            return defaultValue;
        }
    }

    private String resolveLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}

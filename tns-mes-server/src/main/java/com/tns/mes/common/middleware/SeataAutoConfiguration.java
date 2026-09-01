package com.tns.mes.common.middleware;

import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Seata 分布式事务客户端自动配置。
 *
 * <p>采用"功能开关 + 默认兼容单体"模式：
 * <ul>
 *   <li>mes.integration.seata.enabled=true: 设置 Seata 系统属性，
 *       seata-spring-boot-starter (1.7.0) 的 GlobalTransactionScanner 自动激活</li>
 *   <li>mes.integration.seata.enabled=false: 此配置类不创建，
 *       分布式事务以本地事务方式运行（best-effort）</li>
 * </ul>
 *
 * <p>降级策略：Seata 初始化失败时记录错误日志，isAvailable()返回false，
 * 不中断应用启动。业务代码可通过 DegradationStrategy.shouldUseFallback("seata")
 * 判断是否应跳过 @GlobalTransactional 注解。</p>
 *
 * <p>本类职责：补充系统属性、提供可用性探针。实际的 TM/RM 注册、
 * AT/TCC 模式管理由 seata-spring-boot-starter 的自动配置完成。</p>
 */
@Configuration
@ConditionalOnProperty(name = "mes.integration.seata.enabled", havingValue = "true")
public class SeataAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SeataAutoConfiguration.class);

    /** 事务分组名，与 seata.service.vgroup-mapping 的 key 对应 */
    private static final String VGROUP = "tns-mes-group";

    @Value("${mes.integration.seata.addr:}")
    private String seataAddr;

    @Value("${mes.integration.nacos.addr:nacos-svc.tns-mes-middleware:8848}")
    private String nacosAddr;

    @Value("${mes.integration.nacos.username:nacos}")
    private String nacosUsername;

    @Value("${mes.integration.nacos.password:nacos}")
    private String nacosPassword;

    private volatile boolean available = false;

    @PostConstruct
    public void init() {
        try {
            // 设置 Seata 系统属性，供 seata-spring-boot-starter 的自动配置使用
            // 事务分组映射: tns-mes-group → default 集群
            if (System.getProperty("seata.service.vgroup-mapping." + VGROUP) == null) {
                System.setProperty("seata.service.vgroup-mapping." + VGROUP, "default");
            }
            // 传输层配置
            if (System.getProperty("seata.transport.type") == null) {
                System.setProperty("seata.transport.type", "TCP");
            }
            if (System.getProperty("seata.transport.server") == null) {
                System.setProperty("seata.transport.server", "NIO");
            }
            // 使用 Nacos 作为注册中心；Seata Server 地址仅作为直连兜底列表。
            if (System.getProperty("seata.registry.type") == null) {
                System.setProperty("seata.registry.type", "nacos");
            }
            System.setProperty("seata.registry.nacos.server-addr", nacosAddr);
            System.setProperty("seata.registry.nacos.group", "SEATA_GROUP");
            System.setProperty("seata.registry.nacos.username", nacosUsername);
            System.setProperty("seata.registry.nacos.password", nacosPassword);
            System.setProperty("seata.config.type", "file");
            // 如果有配置地址，设置为 direct 模式兜底
            if (seataAddr != null && !seataAddr.isEmpty()) {
                System.setProperty("seata.service.grouplist.default", seataAddr);
                log.info("Seata server address configured: {}", seataAddr);
            }

            // 验证 RootContext 可用
            boolean inTx = RootContext.inGlobalTransaction();
            log.info("Seata initialized: vgroup={}, inGlobalTransaction={}", VGROUP, inTx);

            available = true;
            log.info("Seata integration initialized (vgroup={}, addr={})", VGROUP, seataAddr);
        } catch (Throwable e) {
            log.error("Seata init failed, continuing without distributed transactions: {}", e.getMessage(), e);
            available = false;
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            // 清理当前线程可能残留的全局事务上下文
            if (RootContext.inGlobalTransaction()) {
                RootContext.unbind();
            }
            log.info("Seata context cleaned on shutdown");
        } catch (Throwable e) {
            log.warn("Seata shutdown error: {}", e.getMessage());
        }
    }

    public boolean isAvailable() {
        return available;
    }
}

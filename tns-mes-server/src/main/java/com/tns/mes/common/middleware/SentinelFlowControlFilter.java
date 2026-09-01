package com.tns.mes.common.middleware;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Sentinel 流控 Filter —— 包装 API 入口实现限流。
 *
 * <p>仅当 mes.integration.sentinel.enabled=true 时激活。
 * 与 {@link SentinelAutoConfiguration} 配合使用，对所有非健康检查/Swagger 路径的
 * 请求执行 SphU.entry("mes-api") 流控检查。</p>
 *
 * <p>Filter 执行顺序为 HIGHEST_PRECEDENCE + 10，位于 RequestContextFilter (HIGHEST_PRECEDENCE)
 * 之后、RateLimitFilter (HIGHEST_PRECEDENCE + 20) 之前，确保在限流计数前完成 Sentinel 资源入口。</p>
 *
 * <p>降级策略：
 * <ul>
 *   <li>BlockException（流控触发）: 返回 HTTP 429 + JSON 错误体 {@code {"code":4292,"message":"Flow control triggered"}}</li>
 *   <li>其他 RuntimeException（Sentinel 异常）: 记录告警日志，放行请求，不阻断业务</li>
 * </ul>
 *
 * <p>使用 javax.servlet API（Spring Boot 2.7.x / Java 11 兼容）。</p>
 *
 * @see SentinelAutoConfiguration
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(name = "mes.integration.sentinel.enabled", havingValue = "true")
public class SentinelFlowControlFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SentinelFlowControlFilter.class);

    /** 流控资源名，与 SentinelAutoConfiguration 中 FlowRule 的 resource 一致 */
    private static final String RESOURCE_NAME = "mes-api";

    /** 429 响应体 JSON */
    private static final String BLOCK_RESPONSE_BODY =
            "{\"code\":4292,\"message\":\"Flow control triggered\"}";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 跳过健康检查和 Swagger 路径，与 RateLimitFilter 保持一致
        if (uri.contains("/actuator/health") || uri.contains("/swagger")
                || uri.contains("/v3/api-docs") || uri.contains("/v2/api-docs")) {
            chain.doFilter(request, response);
            return;
        }

        Entry entry = null;
        try {
            // 进入 Sentinel 上下文，origin 为客户端 IP，用于来源识别
            ContextUtil.enter(RESOURCE_NAME, request.getRemoteAddr());
            // 创建流控入口，触发 QPS 统计和规则检查
            entry = SphU.entry(RESOURCE_NAME);
        } catch (BlockException e) {
            // 流控触发：返回 429
            log.warn("Sentinel flow control blocked: uri={}, remoteAddr={}", uri, request.getRemoteAddr());
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(BLOCK_RESPONSE_BODY);
            try { ContextUtil.exit(); } catch (Exception ex) { /* ignore cleanup error */ }
            return;
        } catch (RuntimeException e) {
            // 降级：Sentinel 抛出非预期异常，放行请求
            log.warn("Sentinel unexpected error, proceeding without flow control: {}", e.getMessage());
            try { ContextUtil.exit(); } catch (Exception ex) { /* ignore cleanup error */ }
            chain.doFilter(request, response);
            return;
        }

        // Sentinel 入口创建成功，执行下游 Filter 链
        try {
            chain.doFilter(request, response);
        } finally {
            // 退出资源入口和上下文，释放统计计数
            if (entry != null) {
                try { entry.exit(); } catch (Exception ex) {
                    log.debug("Sentinel entry exit error: {}", ex.getMessage());
                }
            }
            try { ContextUtil.exit(); } catch (Exception ex) {
                log.debug("Sentinel context exit error: {}", ex.getMessage());
            }
        }
    }
}

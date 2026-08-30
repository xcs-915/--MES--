package com.tns.mes.common.web;

import com.tns.mes.common.redis.IdempotencyService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class IdempotencyFilter extends OncePerRequestFilter {
    private final IdempotencyService service;
    public IdempotencyFilter(IdempotencyService service) { this.service = service; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod()) || "PATCH".equalsIgnoreCase(request.getMethod())) {
            if (!request.getRequestURI().contains("/api/v1/auth/login")) {
                String key = request.getHeader("X-Idempotency-Key");
                if (StringUtils.hasText(key) && !service.tryAcquire(request.getMethod() + ":" + request.getRequestURI(), key)) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":4093,\"message\":\"Duplicate request\"}");
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}


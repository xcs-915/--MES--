package com.tns.mes.common.web;

import com.tns.mes.common.redis.RateLimitService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitService service;
    public RateLimitFilter(RateLimitService service) { this.service = service; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().contains("/actuator/health") || request.getRequestURI().contains("/swagger")) {
            chain.doFilter(request, response);
            return;
        }
        String client = request.getRemoteAddr() + ":" + request.getRequestURI();
        if (!service.allow(client)) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":4291,\"message\":\"Too many requests\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}

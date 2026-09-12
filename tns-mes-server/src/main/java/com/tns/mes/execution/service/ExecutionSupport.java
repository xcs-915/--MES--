package com.tns.mes.execution.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.common.exception.BizException;
import com.tns.mes.production.domain.WorkOrder;
import com.tns.mes.production.domain.WorkOrderOperation;
import com.tns.mes.production.repo.WorkOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ExecutionSupport {
    private static final DateTimeFormatter EXTERNAL_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final WorkOrderRepository workOrders;

    public ExecutionSupport(WorkOrderRepository workOrders) { this.workOrders = workOrders; }

    public WorkOrder executableOrder(Long id) {
        WorkOrder order = workOrders.findWithRelationsById(id).orElseThrow(() -> new BizException(4041, "error.not-found"));
        if (!"RELEASED".equals(order.getStatus()) && !"IN_PROGRESS".equals(order.getStatus())) {
            throw new BizException(4092, "error.invalid-state");
        }
        return order;
    }

    public WorkOrder lockOrder(Long id) {
        return workOrders.findLockedById(id).orElseThrow(() -> new BizException(4041, "error.not-found"));
    }

    public WorkOrderOperation operation(WorkOrder order, Long id) {
        if (id == null) return null;
        return order.getOperations().stream().filter(value -> id.equals(value.getId())).findFirst()
                .orElseThrow(() -> new BizException(4003, "error.validation"));
    }

    public static String documentNo(String prefix) {
        String stamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        return prefix + stamp + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public static String externalTime(Instant value) {
        return EXTERNAL_TIME.format(LocalDateTime.ofInstant(value, ZoneId.systemDefault()));
    }

    public static String resultText(JsonNode root, String... names) {
        JsonNode result = root == null ? null : root.path("result");
        for (String name : names) {
            JsonNode value = result == null ? null : result.get(name);
            if (value == null && root != null) value = root.get(name);
            if (value != null && !value.isNull() && !value.asText().trim().isEmpty()) return value.asText();
        }
        return null;
    }

    public static PageRequest page(int page, int size, String sort) {
        return PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size)), Sort.by(Sort.Direction.DESC, sort));
    }

    public static <E, V> PageResponse<V> map(Page<E> page, Function<E, V> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).collect(Collectors.toList()),
                page.getTotalElements(), page.getNumber(), page.getSize(), page.getTotalPages());
    }
}

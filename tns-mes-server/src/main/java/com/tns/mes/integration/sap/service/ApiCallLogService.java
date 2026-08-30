package com.tns.mes.integration.sap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tns.mes.common.api.PageResponse;
import com.tns.mes.integration.sap.domain.ApiCallLog;
import com.tns.mes.integration.sap.repository.ApiCallLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ApiCallLogService {

    private static final int MAX_RESPONSE_BODY_LENGTH = 20000;

    private final ApiCallLogRepository repository;
    private final ObjectMapper mapper;

    public ApiCallLogService(ApiCallLogRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Async
    public void logCall(String systemCode, String endpoint, String httpMethod,
                        Map<String, ?> requestParams, String requestBody,
                        Integer responseStatus, String responseBody,
                        Long durationMs, boolean success, String errorMessage) {
        ApiCallLog log = new ApiCallLog();
        log.setSystemCode(systemCode);
        log.setEndpoint(endpoint);
        log.setHttpMethod(httpMethod);
        log.setRequestParams(toJson(requestParams));
        log.setRequestBody(requestBody);
        log.setResponseStatus(responseStatus);
        log.setResponseBody(truncate(responseBody, MAX_RESPONSE_BODY_LENGTH));
        log.setDurationMs(durationMs);
        log.setSuccess(success);
        log.setErrorMessage(errorMessage);
        log.setCreatedAt(LocalDateTime.now());
        repository.save(log);
    }

    public PageResponse<ApiCallLog> findLatest(int page, int size) {
        Page<ApiCallLog> result = repository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result);
    }

    public PageResponse<ApiCallLog> findByEndpoint(String endpoint, int page, int size) {
        Page<ApiCallLog> result = repository.findByEndpointContainingIgnoreCase(
                endpoint,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result);
    }

    public PageResponse<ApiCallLog> findByFilters(String systemCode, String endpoint, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ApiCallLog> result;
        boolean hasSystem = systemCode != null && !systemCode.trim().isEmpty();
        boolean hasEndpoint = endpoint != null && !endpoint.trim().isEmpty();
        if (hasSystem && hasEndpoint) {
            result = repository.findBySystemCodeAndEndpointContainingIgnoreCase(systemCode, endpoint, pageRequest);
        } else if (hasSystem) {
            result = repository.findBySystemCode(systemCode, pageRequest);
        } else if (hasEndpoint) {
            result = repository.findByEndpointContainingIgnoreCase(endpoint, pageRequest);
        } else {
            result = repository.findAll(pageRequest);
        }
        return PageResponse.from(result);
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException ex) { return String.valueOf(value); }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}

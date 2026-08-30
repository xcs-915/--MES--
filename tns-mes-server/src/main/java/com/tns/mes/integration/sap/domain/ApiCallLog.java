package com.tns.mes.integration.sap.domain;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "int_api_call_log")
public class ApiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_code", length = 40)
    private String systemCode;

    @Column(length = 500)
    private String endpoint;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "request_params", columnDefinition = "nvarchar(max)")
    private String requestParams;

    @Column(name = "request_body", columnDefinition = "nvarchar(max)")
    private String requestBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "nvarchar(max)")
    private String responseBody;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

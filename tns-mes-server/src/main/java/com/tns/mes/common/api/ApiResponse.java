package com.tns.mes.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private String requestId;
    private Instant timestamp;

    public ApiResponse() { }

    private ApiResponse(int code, String message, T data, String requestId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.requestId = requestId;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>(0, "OK", data, requestId);
    }

    public static <T> ApiResponse<T> ok(T data) { return ok(data, null); }

    public static <T> ApiResponse<T> error(int code, String message, String requestId) {
        return new ApiResponse<>(code, message, null, requestId);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}


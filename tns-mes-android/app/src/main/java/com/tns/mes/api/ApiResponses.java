package com.tns.mes.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * MES 后端通用响应体与接口定义相关数据模型。
 *
 * <p>统一返回体约定（若后端采用）：
 * <pre>
 * {
 *   "code": 200,
 *   "msg": "success",
 *   "data": { ... }
 * }
 * </pre>
 */
public final class ApiResponses {

    private ApiResponses() {
        // 工具类，禁止实例化
    }

    /**
     * 通用统一返回体包装。
     *
     * @param <T> data 字段的具体类型
     */
    public static class ApiResult<T> {

        @SerializedName("code")
        private Integer code;

        @SerializedName("msg")
        private String msg;

        @SerializedName("message")
        private String message;

        @SerializedName("data")
        private T data;

        public boolean isSuccessful() {
            return code != null && code == 200;
        }

        public Integer getCode() {
            return code;
        }

        public String getMessage() {
            return msg != null ? msg : message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }

    /**
     * 接口定义查询请求体。
     */
    public static class InterfaceRequest {

        @SerializedName("interfaceCode")
        private String interfaceCode;

        public InterfaceRequest(String interfaceCode) {
            this.interfaceCode = interfaceCode;
        }

        public String getInterfaceCode() {
            return interfaceCode;
        }
    }

    /**
     * 接口定义模型（对应 tns-mes 后端的接口定义表）。
     */
    public static class InterfaceDefinition {

        @SerializedName("id")
        private Long id;

        @SerializedName("interfaceCode")
        private String interfaceCode;

        @SerializedName("interfaceName")
        private String interfaceName;

        @SerializedName("interfaceUrl")
        private String interfaceUrl;

        @SerializedName("method")
        private String method;

        @SerializedName("requestMethod")
        private String requestMethod;

        @SerializedName("description")
        private String description;

        @SerializedName("enabled")
        private Boolean enabled;

        public String getInterfaceCode() {
            return interfaceCode;
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        /** 返回实际的 HTTP 请求方法（兼容 method / requestMethod 两种字段名） */
        public String getMethod() {
            return method != null ? method : requestMethod;
        }

        public String getInterfaceUrl() {
            return interfaceUrl;
        }

        public String getDescription() {
            return description;
        }

        public boolean isEnabled() {
            return enabled == null || enabled;
        }
    }

    /**
     * 上料校验请求体（占位，待后端上料校验接口确定后补充字段）。
     */
    public static class LoadingCheckRequest {

        @SerializedName("workOrderNo")
        private String workOrderNo;

        @SerializedName("materialLabel")
        private String materialLabel;

        public LoadingCheckRequest(String workOrderNo, String materialLabel) {
            this.workOrderNo = workOrderNo;
            this.materialLabel = materialLabel;
        }

        public String getWorkOrderNo() {
            return workOrderNo;
        }

        public String getMaterialLabel() {
            return materialLabel;
        }
    }

    /**
     * 上料校验结果（占位）。
     */
    public static class LoadingCheckResult {

        @SerializedName("passed")
        private Boolean passed;

        @SerializedName("message")
        private String message;

        @SerializedName("materialName")
        private String materialName;

        @SerializedName("specification")
        private String specification;

        @SerializedName("quantity")
        private Double quantity;

        public boolean isPassed() {
            return passed != null && passed;
        }

        public String getMessage() {
            return message;
        }
    }

    /** 通用列表返回体 */
    public static class ApiListResult<T> {

        @SerializedName("code")
        private Integer code;

        @SerializedName("msg")
        private String msg;

        @SerializedName("data")
        private List<T> data;

        public boolean isSuccessful() {
            return code != null && code == 200;
        }

        public List<T> getData() {
            return data;
        }
    }
}

package com.tns.mes.integration.sap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mes.integration.sap")
public class SapProperties {
    private boolean enabled;
    private boolean scheduleEnabled;
    private String baseUrl = "https://my200683.s4hana.sapcloud.cn";
    private String productPath = "/sap/opu/odata/sap/API_PRODUCT_SRV/A_Product";
    private String workOrderPath = "/sap/opu/odata/sap/API_PRODUCTION_ORDER_2_SRV/A_ProductionOrder_2";
    private String componentPath;
    private String operationPath = "/sap/opu/odata/sap/YY1_C_POOPERATIONS_CDS/YY1_C_POOperations";
    private String batchPath = "/sap/opu/odata/sap/API_BATCH_SRV/Batch";
    private String username;
    private String password;
    private String bearerToken;
    private int pageSize = 200;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public boolean isScheduleEnabled() { return scheduleEnabled; }
    public void setScheduleEnabled(boolean value) { scheduleEnabled = value; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String value) { baseUrl = value; }
    public String getProductPath() { return productPath; }
    public void setProductPath(String value) { productPath = value; }
    public String getWorkOrderPath() { return workOrderPath; }
    public void setWorkOrderPath(String value) { workOrderPath = value; }
    public String getComponentPath() { return componentPath; }
    public void setComponentPath(String value) { componentPath = value; }
    public String getOperationPath() { return operationPath; }
    public void setOperationPath(String value) { operationPath = value; }
    public String getBatchPath() { return batchPath; }
    public void setBatchPath(String value) { batchPath = value; }
    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public String getBearerToken() { return bearerToken; }
    public void setBearerToken(String value) { bearerToken = value; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int value) { pageSize = value; }
}

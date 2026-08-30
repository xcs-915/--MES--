package com.tns.mes.basic.domain;

import java.util.Arrays;

public enum MasterDataType {
    ENTERPRISE("enterprise"), FACTORY("factory"), WORKSHOP("workshop"), DEPARTMENT("department"),
    WAREHOUSE("warehouse"), WORK_CENTER("work-center"), PRODUCTION_LINE("production-line"),
    WORKSTATION("workstation"), PERSON("person"), POSITION("position"), CUSTOMER("customer"),
    SUPPLIER("supplier"), MANUFACTURER("manufacturer");

    private final String code;
    MasterDataType(String code) { this.code = code; }
    public String getCode() { return code; }

    public static MasterDataType parse(String value) {
        return Arrays.stream(values()).filter(type -> type.code.equalsIgnoreCase(value)
                        || type.name().equalsIgnoreCase(value.replace('-', '_')))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported master data type: " + value));
    }
}


package com.tiger.rpc.common.enums;

/**
 * @ClassName: ServiceErrorCodeEnum.java
 *
 * @Description: 
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/23
 */
public enum ServiceCodeEnum {

    PROVIDER_NOT_FOUND("PROVIDER_NOT_FOUND", "Service[%s] provider[%s] not found.", "服务【%s】提供者【%s】未找到"),

    SERVICE_NOT_FOUND("SERVICE_NOT_FOUND", "Service[%s] not found.", "服务【%s】未找到"),

    SERVICE_NO_AVAILABLE_PROVIDERS("SERVICE_NO_AVAILABLE_PROVIDERS", "Service[%s] has no providers for this consumer", "服务【%s】对该引用机器没有可用提供者"),

    MISS_REQUIRED_PARAMETER("MISS_REQUIRED_PARAMETER", "Miss required parameter[%s]", "缺失必选参数 (%s)"),

    ILLEGAL_PARAMETER("ILLEGAL_PARAMETER", "Illegal parameter[%s]", "非法参数 (%s)"),

    DISCOVERY_NOT_INITIALIZED("DISCOVERY_NOT_INITIALIZED", "Services discovery not initialized", "服务发现器为开启"),

    PROVIDER_URI_NOT_ILLEGAL("URI_NOT_ILLEGAL", "provider uri[%s] illegal", "服务提供者uri[%s]非法"),

    INTERFACE_NOT_IMPORT("INTERFACE_NOT_IMPORT", "Iface[%s] not ", "接口【%s】未引入"),

    SYSTEM_ERROR("SYSTEM_ERROR", "System error", "系统异常"),

    ;

    private String code;
    private String value;
    private String description;

    ServiceCodeEnum(String code, String value, String description) {
        this.code = code;
        this.value = value;
        this.description = description;
    }

    public String getCode() {
        return this.code;
    }

    public String getValue() {
        return this.value;
    }

    public String getDescription() {
        return this.description;
    }


}

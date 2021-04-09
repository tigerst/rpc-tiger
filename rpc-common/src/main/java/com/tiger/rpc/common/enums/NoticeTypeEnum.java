package com.tiger.rpc.common.enums;

/**
 * @ClassName: NoticeTypeEnum.java
 *
 * @Description: 通知类型枚举类
 *
 * @Author: Tiger
 *
 * @Date: 2019/5/10
 */
public enum NoticeTypeEnum {

    //服务异常
    PROTOCOL_SERVICE_ABNORMAL("SERVICE_ABNORMAL", "Protocol[%s] Service of provider abnormal notice", "Protocol[%s]服务异常通知"),

    //机器异常
    PROVIDER_ABNORMAL("PROVIDER_ABNORMAL", "Provider abnormal notice", "机器异常通知"),

    INSUFFICIENT_RESOURCE("INSUFFICIENT_RESOURCE", "Insufficient resource notice", "资源不足通知"),

    SERVICE_DISABLED("SERVICE_DISABLED", "Service disabled notice", "服务被停用通知"),

    SERVICE_ENABLED("SERVICE_ENABLED", "Service enabled notice", "服务启用通知"),

    PROVIDER_DISABLED("PROVIDER_DISABLED", "Provider disabled notice", "机器被停用通知"),

    PROVIDER_ENABLED("PROVIDER_ENABLED", "Provider enabled notice", "机器启用通知"),

    PROTOCOL_SERVER_DISABLED("PROTOCOL_SERVER_DISABLED", "Protocol[%s] server disabled notice", "Protocol[%s]服务进程停用通知"),

    PROTOCOL_SERVER_ENABLED("PROTOCOL_SERVER_ENABLED", "Protocol[%s] server enabled notice", "Protocol[%s]服务进程启用通知"),

    ;

    private String code;
    private String value;
    private String description;

    NoticeTypeEnum(String code, String value, String description) {
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

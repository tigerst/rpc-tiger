package com.tiger.rpc.common.enums;

/**
 * @ClassName: ProtocolTypeEnum.java
 *
 * @Description: 协议类型
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/23
 */
public enum ProtocolTypeEnum {

    THRIFT("thrift", "thrift协议"),

    //http get
    HTTP_GET("httpGET", "http get协议"),

    //http post
    HTTP_POST("httpPOST", "http post协议"),

    NETTY("netty", "netty协议"),

    ;

    private String value;
    private String description;

    ProtocolTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return this.value;
    }

    public String getDescription() {
        return this.description;
    }

}

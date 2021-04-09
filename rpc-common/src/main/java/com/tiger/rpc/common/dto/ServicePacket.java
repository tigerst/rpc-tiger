package com.tiger.rpc.common.dto;

import com.tiger.rpc.common.enums.ServiceStatus;
import lombok.Data;


/**
 * @ClassName: SimpleServiceConfig.java
 *
 * @Description: 服务信息包，用于传输外部
 *
 * @Author: Tiger
 *
 * @Date: 2019/5/6
 */
@Data
public class ServicePacket {

    /**
     * 服务的端口
     */
    private int port;

    /**
     * 服务的协议类型: THRIFT（thrift）、HTTP_GET（httpGET）、HTTP_POST（httpPOST）、NETTY（netty）
     */
    private String protocol;

    /**
     * 服务的接口类型
     */
    private String interfaceName;

    /**
     * 服务的版本号
     */
    private String version = "1.0.0";

    /**
     * url路径
     */
    private String url;

    /**
     * 服务的状态：ENABLED(1)、DISABLED(2)
     */
    private ServiceStatus serviceStatus;

}

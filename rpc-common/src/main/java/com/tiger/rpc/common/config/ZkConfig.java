package com.tiger.rpc.common.config;

import lombok.Builder;
import lombok.Data;

/**
 * @ClassName: ZkConfig.java
 *
 * @Description: zk配置
 *
 * @Author: Tiger
 *
 * @Date: 2019/4/28
 */
@Data
@Builder( toBuilder = true)
public class ZkConfig {

    /**
     * namespace
     */
    private String namespace;

    /**
     * zkServers
     */
    private String zkServers;

    /**
     * session超时
     */
    private int sessionTimeOut;

    /**
     * 连接超时
     */
    private int connectionTimeOut;

    /**
     * 重试间隔
     */
    private int retryInterval;

    /**
     * 重试次数
     */
    private int retryTimes;

}

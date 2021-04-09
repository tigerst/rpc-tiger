package com.tiger.rpc.common.utils;

import com.alibaba.fastjson.JSON;
import com.tiger.rpc.common.config.ZkConfig;
import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * @ClassName: ZkUtils.java
 *
 * @Description: zk工具
 *
 * @Author: Tiger
 *
 * @Date: 2019/4/28
 */
@Slf4j
public class ZkUtils {

    /**
     * zk配置校验
     * @param zkConfig
     */
    public static void validateZkConfig(ZkConfig zkConfig) {
        Preconditions.checkArgument(StringUtils.isNotBlank(zkConfig.getNamespace()),
                String.format("zkConfig.namespace[%s] is illegal", zkConfig.getNamespace()));
        Preconditions.checkArgument(StringUtils.isNotBlank(zkConfig.getZkServers()),
                String.format("zkConfig.zkServers[%s] is illegal", zkConfig.getZkServers()));
        Preconditions.checkArgument(zkConfig.getSessionTimeOut() > 0,
                String.format("zkConfig.sessionTimeOut[%d] is illegal", zkConfig.getSessionTimeOut()));
        Preconditions.checkArgument(zkConfig.getConnectionTimeOut() > 0,
                String.format("zkConfig.connectionTimeOut[%d] is illegal", zkConfig.getConnectionTimeOut()));
        Preconditions.checkArgument(zkConfig.getRetryInterval() >= 0,
                String.format("zkConfig.retryInterval[%d] is illegal", zkConfig.getRetryInterval()));
        Preconditions.checkArgument(zkConfig.getRetryTimes() >= 0,
                String.format("zkConfig.retryTimes[%d] is illegal", zkConfig.getRetryTimes()));
    }

    /**
     * 创建无namespace的zkClient，用于soa的服务
     * @return
     */
    public static CuratorFramework createZkClientWithoutNameSpace(ZkConfig zkConfig) {
        //校验参数
        validateZkConfig(zkConfig);
        //不含有namespace，直接创建在根路径上
        CuratorFramework zkClient = CuratorFrameworkFactory.builder()
                .connectString(zkConfig.getZkServers())
                .sessionTimeoutMs(zkConfig.getSessionTimeOut())
                .connectionTimeoutMs(zkConfig.getConnectionTimeOut())
                .retryPolicy(new ExponentialBackoffRetry(zkConfig.getRetryInterval(), zkConfig.getRetryTimes()))
                .build();
        //启动zk
        zkClient.start();
        log.debug("Non-namespace zkClient[{}] create successfully", JSON.toJSONString(zkConfig));
        return zkClient;
    }

    /**
     * 创建代有namespace的zk客户端
     * @param zkConfig
     * @return
     */
    public static CuratorFramework createZkClientWithNameSpace(ZkConfig zkConfig){
        //校验参数
        validateZkConfig(zkConfig);
        //含有namespace，创建在namespace路径上
        CuratorFramework zkClient = CuratorFrameworkFactory.builder()
                .namespace(zkConfig.getNamespace())
                .connectString(zkConfig.getZkServers())
                .sessionTimeoutMs(zkConfig.getSessionTimeOut())
                .connectionTimeoutMs(zkConfig.getConnectionTimeOut())
                .retryPolicy(new ExponentialBackoffRetry(zkConfig.getRetryInterval(), zkConfig.getRetryTimes()))
                .build();
        //启动zk
        zkClient.start();
        log.debug("Namespace zkClient[{}] create successfully", JSON.toJSONString(zkConfig));
        return zkClient;
    }


    public static void main(String[] args) {
        ZkConfig zkConfig =  ZkConfig.builder().namespace("hha").zkServers("hha").sessionTimeOut(3000).connectionTimeOut(4000)
                .retryInterval(3000).retryTimes(3).build();
        validateZkConfig(zkConfig);
    }

}

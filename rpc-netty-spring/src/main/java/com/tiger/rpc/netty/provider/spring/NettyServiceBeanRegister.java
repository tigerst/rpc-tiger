package com.tiger.rpc.netty.provider.spring;

import com.tiger.rpc.common.config.MonitorConfig;
import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.common.provider.NoticeService;
import com.tiger.rpc.common.provider.SyncMachineService;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.netty.provider.NettyServiceRegister;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * @ClassName: NettyServiceBeanRegister.java
 *
 * @Description: spring方式管理注册服务
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/31
 */
@Slf4j
public class NettyServiceBeanRegister extends NettyServiceRegister implements InitializingBean, DisposableBean {


    public NettyServiceBeanRegister(ApplicationRegister appRegister, List<ServiceConfig> serviceList, int selectorThreads, int workerThreads, int serverPort) {
        super(appRegister, serviceList, selectorThreads, workerThreads, serverPort);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //spring方式管理，启动
        super.register();
    }

    @Override
    public void destroy() throws Exception {
        //spring方式，销毁容器
        super.unRegister();
        //清空服务
        super.getServiceList().clear();
        //清空bean
        super.getServiceBeanMap().clear();
    }
}

package com.tiger.rpc.thrift.provider.spring;

import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.thrift.provider.ThriftServiceRegister;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * @ClassName: ThriftServiceBeanRegister.java
 *
 * @Description: spring方式管理注册服务
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/23
 */
@Slf4j
public class ThriftServiceBeanRegister extends ThriftServiceRegister implements InitializingBean, DisposableBean {

    public ThriftServiceBeanRegister(ApplicationRegister appRegister, List<ServiceConfig> serviceList, int selectorThreads, int workerThreads, int serverPort) {
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

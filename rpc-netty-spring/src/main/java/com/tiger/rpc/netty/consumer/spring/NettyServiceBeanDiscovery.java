package com.tiger.rpc.netty.consumer.spring;

import com.tiger.rpc.common.config.ReferenceConfig;
import com.tiger.rpc.common.consumer.policy.RoundRobinStrategy;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.netty.consumer.NSocket;
import com.tiger.rpc.netty.consumer.NettyServiceDiscovery;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * @ClassName: NettyServiceBeanDiscovery.java
 *
 * @Description: spring 方式管理发现服务
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/31
 */
public class NettyServiceBeanDiscovery extends NettyServiceDiscovery implements InitializingBean, DisposableBean {

    public NettyServiceBeanDiscovery(){
        super();
    }

    public NettyServiceBeanDiscovery(ApplicationRegister appRegister, List<ReferenceConfig> referenceList, GenericKeyedObjectPool<String, NSocket> pool) {
        super(appRegister, referenceList, new RoundRobinStrategy(), pool);
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        //spring方式管理，启动
        super.discovery();
    }

    @Override
    public void destroy() throws Exception {
        super.unDiscovery();
        //清空资源
        super.getServiceProvidersMap().clear();
        super.getReferenceBeanMap().clear();
    }

}

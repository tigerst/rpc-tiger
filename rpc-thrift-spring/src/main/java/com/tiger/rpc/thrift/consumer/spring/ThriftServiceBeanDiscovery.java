package com.tiger.rpc.thrift.consumer.spring;

import com.tiger.rpc.common.config.ReferenceConfig;
import com.tiger.rpc.common.consumer.policy.RoundRobinStrategy;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.thrift.consumer.ThriftServiceDiscovery;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.thrift.transport.TSocket;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

/**
 * @ClassName: ThriftServiceBeanDiscovery.java
 *
 * @Description: spring 方式管理发现服务
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/23
 */
public class ThriftServiceBeanDiscovery extends ThriftServiceDiscovery implements InitializingBean, DisposableBean {

    public ThriftServiceBeanDiscovery(){
        super();
    }

    public ThriftServiceBeanDiscovery(ApplicationRegister appRegister, List<ReferenceConfig> referenceList, GenericKeyedObjectPool<String, TSocket> pool) {
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

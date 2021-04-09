package com.tiger.rpc.netty.consumer.spring;

import com.tiger.rpc.common.config.ReferenceConfig;
import com.tiger.rpc.common.enums.ServiceCodeEnum;
import com.tiger.rpc.common.exception.ServiceException;
import com.tiger.rpc.netty.consumer.handler.NettyDefaultHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import java.lang.reflect.Proxy;

/**
 * @ClassName: NettyReferenceProxyFactoryBean.java
 *
 * @Description: 应用服务代理工厂类
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/31
 */
@Data
@Slf4j
public class NettyReferenceProxyFactoryBean<T> implements FactoryBean<T>, InitializingBean, DisposableBean {

    /**
     * 引入发现服务
     */
    private NettyServiceBeanDiscovery discovery;

    /**
     * 接口类
     */
    private Class<T> iFaceInterface;

    /**
     * 接口代理实例
     */
    private T proxyClient;

    @Override
    public T getObject() throws Exception {
        if(this.proxyClient == null){
            //不存在初始化
            init();
        }
        return this.proxyClient;
    }

    @Override
    public Class<?> getObjectType() {
        return iFaceInterface;
    }

    @Override
    public void destroy() throws Exception {
        this.discovery = null;
        this.iFaceInterface = null;
        this.proxyClient = null;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        if(this.discovery == null){
            //服务发现器校验
            throw new ServiceException(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getCode(),
                    String.format(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getValue(), "discovery"));
        }
        if(!this.discovery.isRegistered()){
            //服务发现器为开启
            throw new ServiceException(ServiceCodeEnum.DISCOVERY_NOT_INITIALIZED.getCode(),
                    ServiceCodeEnum.DISCOVERY_NOT_INITIALIZED.getValue());
        }
        //初始化
        init();
    }

    /**
     * 初始化proxy对象
     * @throws ServiceException
     */
    private void init() throws ServiceException {
        //获取接口配置
        ReferenceConfig config = discovery.getConfbyInterfaceClass(this.iFaceInterface);
        if(config == null){
            //接口未引入
            throw new ServiceException(ServiceCodeEnum.INTERFACE_NOT_IMPORT.getCode(),
                    String.format(ServiceCodeEnum.INTERFACE_NOT_IMPORT.getValue(), iFaceInterface.getName()));
        }
        if(config.getProxy() != null){
            //如果代理已经存在，则直接从代理中选择
            this.proxyClient = (T) config.getProxy();
            return;
        }
        NettyDefaultHandler handler = new NettyDefaultHandler(discovery);
        //设置连接池
        handler.setPool(this.discovery.getPool());
        //设置服务版本号
        handler.setServiceVersion(config.getVersion());
        if(config.getRetry() > 0){
            //设置重试次数
            handler.setRetry(config.getRetry());
        }
        //获取类加载器
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        //创建代理实例
        this.proxyClient = (T) Proxy.newProxyInstance(classLoader, new Class[] { iFaceInterface }, handler);
    }
}

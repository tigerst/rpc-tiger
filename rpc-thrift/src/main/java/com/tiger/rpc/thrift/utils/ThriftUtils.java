package com.tiger.rpc.thrift.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * @ClassName: ThriftUtils.java
 *
 * @Description: thrift工具类
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/29
 */
@Slf4j
public class ThriftUtils {

    /**
     * 获取服务的工厂类
     * @param iFaceInterface
     * @return
     */
    public static TServiceClientFactory<TServiceClient> getClientFactory(Class<?> iFaceInterface) {
        TServiceClientFactory<TServiceClient> clientFactory = null;
        try {
            //通过thrift协议类， 获取thrift协议的client factory
            clientFactory = (TServiceClientFactory<TServiceClient>) iFaceInterface.getClassLoader()
                    .loadClass(iFaceInterface.getDeclaringClass().getName() + "$Client$Factory").newInstance();
        } catch (Exception e) {
            log.error("Obtain factoryClass error", e);
        }
        return clientFactory;
    }

    /**
     * 通过反射创建processor
     * @param bean
     * @param ifaceClazz
     * @return
     */
    public static TProcessor getServiceProcessor(Object bean, Class<?> ifaceClazz) {
        Class<TProcessor> processorClazz = null;
        Class<?> clazz = null;
        try {
            clazz = ifaceClazz.getDeclaringClass();
            //所有声明类
            Class<?>[] classes = clazz.getDeclaredClasses();
            for (Class<?> innerClazz : classes) {
                if (innerClazz.getName().endsWith("$Processor") && TProcessor.class.isAssignableFrom(innerClazz)) {
                    //获取名字为Processor的类
                    processorClazz = (Class<TProcessor>) innerClazz;
                    break;
                }
            }
            if (processorClazz == null) {
                throw new IllegalStateException("No TProcessor Found.");
            }
            Constructor<TProcessor> constructor = processorClazz.getConstructor(ifaceClazz);
            if (!Modifier.isPublic(constructor.getModifiers()) || !Modifier.isPublic(constructor.getDeclaringClass().getModifiers())) {
                //把contructor设置成accessible，确保可使用构造器创建对象
                if(!constructor.isAccessible()){
                    //构造器为私有时设置成公有的
                    constructor.setAccessible(true);
                }
            }
            //使用构造器创建实例
            return constructor.newInstance(bean);
        } catch (Exception e) {
            log.error("Get serviceProcessor[{}] error", processorClazz, e);
        }
        return null;
    }

}

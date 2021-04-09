package com.tiger.rpc.common.utils;

import com.google.common.collect.Lists;
import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.common.register.ServiceRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @ClassName: ProviderParser.java
 *
 * @Description: provider解析器
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/28
 */
public class ProviderParser {

    /**
     * 解析单个服务提供者
     * @param provider
     * @return
     */
    public static List<String> parseSingleProvider(String provider) {
        List<String> addressList = new ArrayList<String>();
        String[] str = provider.split(Constants.HOST_PORT_SEPARATOR);
        if (str.length == 3) {
            int weight = Integer.valueOf(str[2]);
            for (int i = 0; i < weight; i++) {
                //权重为多少是，生成多少个实例
                addressList.add(str[0] + Constants.HOST_PORT_SEPARATOR + str[1]);
            }
        }
        return addressList;
    }

    /**
     * 批量解析服务提供者
     * @param providers
     * @return
     */
    public static List<String> parseProviders(List<String> providers){
        //并发容器
        List<String> addressList = Lists.newArrayList();
        if (providers == null || providers.size() == 0) {
            return addressList;
        }
        for (String provider : providers) {
            String[] str = provider.split(Constants.HOST_PORT_SEPARATOR);
            if (str.length == 3) {
                int weight = Integer.valueOf(str[2]);
                for (int i = 0; i < weight; i++) {
                    addressList.add(str[0] + Constants.HOST_PORT_SEPARATOR + str[1]);
                }
            }
        }
        //乱序列表
        Collections.shuffle(addressList);
        return addressList;
    }

    /**
     * 组装服务提供者路径：appPath/protocol(thrift/netty)/com.tiger.chaos.xxx_1.0.0(服务名)/providers/127.0.0.0:8081:3
     * @param serviceConfig
     */
    public static String assembleProviderPath(String appPath, ServiceConfig serviceConfig) {
        StringBuffer sb = new StringBuffer();
        //设置应用路径
        sb.append(appPath);
        //服务地址: appPath/protocol(thrift/netty)/com.tiger.chaos.UserService_1.0.0(服务名)
        Class<?> enClosedClazz = serviceConfig.getInterfaceClass().getEnclosingClass();
        enClosedClazz = enClosedClazz == null ? serviceConfig.getInterfaceClass() : enClosedClazz;
        sb.append(Constants.PATH_SEPARATOR).append(serviceConfig.getProtocol()).append(Constants.PATH_SEPARATOR)
                .append(enClosedClazz.getName()).append(Constants.SERVICE_VERSION_SEPARATOR).append(serviceConfig.getVersion());
        //机器地址：servicePath/providers/127.0.0.0:8081:3
        sb.append(Constants.PATH_SEPARATOR).append("providers").append(Constants.PATH_SEPARATOR)
                .append(serviceConfig.getHost()).append(Constants.HOST_PORT_SEPARATOR).append(serviceConfig.getPort()).append(Constants.HOST_PORT_SEPARATOR)
                .append(serviceConfig.getWeight());
        return sb.toString();
    }

    /**
     * 组装服务路径：appPath/protocol(thrift/netty)/com.tiger.chaos.xxx_1.0.0(服务名)
     * @param serviceConfig
     */
    public static String assembleServicePath(String appPath, ServiceConfig serviceConfig) {
        StringBuffer sb = new StringBuffer();
        //设置应用路径
        sb.append(appPath);
        //服务地址: appPath/protocol(thrift/netty)/com.tiger.chaos.UserService_1.0.0(服务名)
        Class<?> enClosedClazz = serviceConfig.getInterfaceClass().getEnclosingClass();
        enClosedClazz = enClosedClazz == null ? serviceConfig.getInterfaceClass() : enClosedClazz;
        sb.append(Constants.PATH_SEPARATOR).append(serviceConfig.getProtocol()).append(Constants.PATH_SEPARATOR)
                .append(enClosedClazz.getName()).append(Constants.SERVICE_VERSION_SEPARATOR).append(serviceConfig.getVersion());
        return sb.toString();
    }

    /**
     * 获取应用上的所有协议的服务
     * @return
     */
    public static List<ServiceConfig> getAllServiceList(ApplicationRegister applicationRegister){
        /**
         * 获取所有注册器的服务
         */
        final List<ServiceConfig> serviceConfigList = new ArrayList<>();
        if (applicationRegister != null) {
            applicationRegister.getServiceRegisters().stream().forEach(o -> {
                serviceConfigList.addAll(o.getServiceList());
            });
        }
        return serviceConfigList;
    }

    /**
     * 获取某个服务注册器上的服务
     * @return
     */
    public static List<ServiceConfig> getServiceListByeProtocolServer(ServiceRegister serviceRegister){
        /**
         * 获取所有注册器的服务
         */
        final List<ServiceConfig> serviceConfigList = new ArrayList<>();
        if (serviceRegister != null) {
            serviceRegister.getServiceList().stream().forEach(o -> {
                serviceConfigList.add(o);
            });
        }
        return serviceConfigList;
    }

}

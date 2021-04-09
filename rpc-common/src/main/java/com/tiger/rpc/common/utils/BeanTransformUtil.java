package com.tiger.rpc.common.utils;

import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.common.dto.ServerPacket;
import com.tiger.rpc.common.dto.ServicePacket;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName: BeanTransformUtil.java
 *
 * @Description: bean转换工具
 *
 * @Author: Tiger
 *
 * @Date: 2018/5/10
 */
public class BeanTransformUtil {

    /**
     * 将服务配置组装成packet列表（方法级别的）
     * @param config
     * @return
     */
    public static List<ServicePacket> transformToServicePackets(ServiceConfig config) {
        List<ServicePacket> servicePacketList = Lists.newArrayList();
        if(config != null){
            Method[] methods = config.getInterfaceClass().getDeclaredMethods();
            Class<?> enClosedClazz = config.getInterfaceClass().getEnclosingClass();
            enClosedClazz = enClosedClazz == null? config.getInterfaceClass() : enClosedClazz;
            if(ArrayUtils.isNotEmpty(methods)){
                ServicePacket servicePacket;
                StringBuffer urlSb = new StringBuffer();
                for (Method method : methods) {
                    urlSb.setLength(0);
                    servicePacket = new ServicePacket();
                    servicePacket.setProtocol(config.getProtocol());
                    //协议服务端口
                    servicePacket.setPort(config.getPort());
                    servicePacket.setInterfaceName(config.getInterfaceName());
                    servicePacket.setVersion(config.getVersion());
                    servicePacket.setServiceStatus(config.getServiceStatus());
                    //拼接接口方法的url：/com.tiger.chaos.UserService_1.0.0(服务名)/方法名;
                    servicePacket.setUrl(urlSb.append(Constants.PATH_SEPARATOR).append(enClosedClazz.getName())
                            .append(Constants.SERVICE_VERSION_SEPARATOR).append(config.getVersion())
                            .append(Constants.PATH_SEPARATOR).append(method.getName()).toString());
                    //加入列表
                    servicePacketList.add(servicePacket);
                }
            }
        }
        return servicePacketList;
    }

    /**
     * 组装主机信息（挂载主机信息和服务列表）
     * @param serviceConfigs
     * @return
     */
    private static ServerPacket transformToServerPacket(List<ServiceConfig> serviceConfigs){
        ServerPacket serverConfig = new ServerPacket();
        if(CollectionUtils.isEmpty(serviceConfigs)) {
            return null;
        }
        for (int i = 0; i < serviceConfigs.size(); i++) {
            if(i == 0){
                serverConfig.getMachinePacket().setIp(serviceConfigs.get(i).getIp());
                serverConfig.getMachinePacket().setHostName(serviceConfigs.get(i).getHostName());
                serverConfig.getMachinePacket().setPid(serviceConfigs.get(i).getPid());
                serverConfig.getMachinePacket().setAvailableProcessors(serviceConfigs.get(i).getAvailableProcessors());
                serverConfig.getMachinePacket().setCpuUsage(serviceConfigs.get(i).getCpuUsage());
                serverConfig.getMachinePacket().setDiskUsage(serviceConfigs.get(i).getDiskUsage());
                serverConfig.getMachinePacket().setOtherMessage(serviceConfigs.get(i).getOtherMessage());
            }
            //组装service packet，并加入列表
            serverConfig.getServicePackets().addAll(transformToServicePackets(serviceConfigs.get(i)));
        }
        return serverConfig;
    }

    /**
     * 通过主机分组，组装成机器信息列表（包含每台机器下的可用服务列表）
     * @param serviceConfigs
     * @return 响应机器信息列表
     */
    public static List<ServerPacket> transformToServerPacketGroupByHost(List<ServiceConfig> serviceConfigs){
        if(CollectionUtils.isEmpty(serviceConfigs)){
            return Lists.newArrayList();
        }
        //存在时，处理，根据host分组
        Map<String, List<ServiceConfig>> hostServicesMap = serviceConfigs.stream().collect(
                Collectors.groupingBy(o -> o.getHost()));
        List<ServerPacket> list = Lists.newArrayList();
        hostServicesMap.entrySet().forEach(entry -> {
            if(CollectionUtils.isNotEmpty(entry.getValue())){
                list.add(transformToServerPacket(entry.getValue()));
            }
        });
        return list;
    }

    /**
     * 组装服务提供者路径：appPath/protocol(thrift/netty)/com.tiger.chaos.xxx_1.0.0(服务名)/providers/127.0.0.0:8081:3
     * @param appPath
     * @param serviceConfig
     * @return
     */
    public String assembleProviderPath(String appPath, ServiceConfig serviceConfig) {
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


}

package com.tiger.rpc.common.helper;

import com.alibaba.fastjson.JSON;
import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.common.dto.ServerPacket;
import com.tiger.rpc.common.enums.NoticeTypeEnum;
import com.tiger.rpc.common.enums.ServiceCodeEnum;
import com.tiger.rpc.common.enums.ServiceStatus;
import com.tiger.rpc.common.exception.ServiceException;
import com.tiger.rpc.common.provider.NoticeService;
import com.tiger.rpc.common.provider.SyncMachineService;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.common.register.ServiceRegister;
import com.tiger.rpc.common.utils.BeanTransformUtil;
import com.tiger.rpc.common.utils.Constants;
import com.tiger.rpc.common.utils.NetworkUtils;
import com.google.common.collect.Lists;
import com.tiger.rpc.common.utils.ProviderParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: ServiceHelper.java
 *
 * @Description: 
 *
 * @Author: Tiger
 *
 * @Date: 2019/4/30
 */
@Slf4j
public class ServiceHelper {

    /**
     * 服务注册器
     */
    private ApplicationRegister applicationRegister;

    /**
     * zk客户端
     */
    private CuratorFramework zkClient;

    /**
     * 服务操作自动告警
     */
    private boolean isAutoAlertForService;


    /**
     * 通知服务
     */
    private NoticeService noticeService;

    /**
     * owner列表
     */
    private List<String> owners = Lists.newArrayList();

    private String appName;

    public ServiceHelper(ApplicationRegister register){
        this.applicationRegister = register;
        this.zkClient = register.getZkClient();
        this.isAutoAlertForService = register.getMonitorConfig().isAutoAlertForService();
        this.noticeService = register.getNoticeService();
        this.owners.addAll(Arrays.asList(register.getAppConf().getOwner().split(Constants.OWNER_SEPARATOR)));
        this.appName = register.getAppConf().getName();
    }

    /**
     * 向节点写入信息
     * @param otherMessage
     */
    public void writeOtherMessage(String otherMessage){
        List<ServiceConfig> serviceList = ProviderParser.getAllServiceList(applicationRegister);
        if(CollectionUtils.isEmpty(serviceList)){
            log.info("No service to found for the application");
            return;
        }
        //遍历服务
        for (ServiceConfig config : serviceList) {
            //设置写入信息
            config.setOtherMessage(otherMessage);
        }

        try {
            //上送配置到线上
            upServiceConfigsToZk();
            //同步到默认定制化地方
//            syncToCustomizedPlace(null);
        } catch (Exception e) {
            log.error("Write otherMessage[{}] for host[{}] error, otherMessage cached, delay do retries automatically, msg: {} ", otherMessage, NetworkUtils.ip(), e);
        }
        
        log.debug("Write other message[{}] successfully", otherMessage);
    }

    /**
     * 向服务配置写入信息，并延迟到下次连接处理
     * @param otherMessage
     */
    public void writeOtherMessageWithDelay(String otherMessage){
        List<ServiceConfig> serviceList = ProviderParser.getAllServiceList(applicationRegister);
        if(CollectionUtils.isEmpty(serviceList)){
            log.info("No service to found for the application");
            return;
        }
        //遍历服务
        for (ServiceConfig config : serviceList) {
            //设置写入信息
            config.setOtherMessage(otherMessage);
        }
        log.debug("Write other message[{}] with delay successfully", otherMessage);
    }

    /**
     * 获取自定义数据
     * @return
     */
    public String getOtherMessage(){
        List<ServiceConfig> serviceList = ProviderParser.getAllServiceList(applicationRegister);
        if(CollectionUtils.isEmpty(serviceList)){
            log.info("No service to register");
            return null;
        }
        //从本地任意一个获取，确保拿到最新的信息
        return serviceList.get(0).getOtherMessage();
    }

    /**
     * 启用该机器的所有服务
     * @return
     */
    /**
     *
     * @param operator
     * @throws Exception
     */
    public void enableCurrentProvider(String operator) throws Exception {
        List<ServiceConfig> serviceList = ProviderParser.getAllServiceList(applicationRegister);
        if(CollectionUtils.isEmpty(serviceList)){
            log.info("No services for this provider to disable");
            return;
        }
        if(CollectionUtils.isEmpty(serviceList)){
            log.info("No services for this provider to disable");
            return;
        }
        log.debug("Start provider by operator[{}]", operator);
        StringBuffer errorSb = new StringBuffer();
        List<String> errorList = Lists.newArrayList();
        String providerPath;
        for (ServiceConfig config : serviceList) {
            try {
                //机器地址：appPath/protocol(thrift/netty)/com.tiger.chaos.xxx_1.0.0(服务名)/providers/127.0.0.0:8081:3
                providerPath = ProviderParser.assembleProviderPath(applicationRegister.getAppPath(), config);
                if(zkClient.checkExists().forPath(providerPath) == null){
                    //创建ephemeral模式的provider地址，并记录service provider信息
                    zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
                            .forPath(providerPath, JSON.toJSONString(config).getBytes(Constants.DEFAULT_CHARSET));
                }
                //设置启用标记
                config.setServiceStatus(ServiceStatus.ENABLED);
                //处理完成退出循环
            } catch (Exception e) {
                log.error("Provider's[{}/{}] service[{}] enabled error, msg: {}", config.getHostName(), config.getIp(),
                        config.getInterfaceClass().getSimpleName(), e);
                errorSb.append(e.getMessage()).append("\n");
                errorList.add(config.getInterfaceClass().getSimpleName());
            }
        }
        if(CollectionUtils.isNotEmpty(errorList)){
            log.error("Provider disabled error \n {}", errorSb.toString());
            String msg = String.format("Provider's[%s/%s] services[%s] disabled error,\n %s", NetworkUtils.host(), NetworkUtils.ip(),
                    StringUtils.join(errorList, ","), errorSb.toString());
            throw new ServiceException(ServiceCodeEnum.SYSTEM_ERROR.getCode(), msg);
        }
        log.debug("Provider started successfully by operator[{}]", operator);
        String msg = String.format("【%s】应用【%s】的提供者【%s/%s】被【%s】启用。",
                DateFormatUtils.format(new Date(), Constants.NOTICE_TIME_PATTERN), appName, NetworkUtils.host(), NetworkUtils.ip() ,operator);
        //告警日志
        log.warn(msg);
        if(isAutoAlertForService && noticeService != null){
            noticeService.notice(owners, NoticeTypeEnum.PROVIDER_ENABLED.getDescription(), msg);
        }
        //无异常同步数据
        syncToCustomizedPlaceWithoutException(operator);
    }

    /**
     * 暂停该机器的指定服务
     * @param interfaceClass   服务接口
     * @param operator  操作人
     * @throws Exception
     */
    public void enableCurrentProviderByService(Class<?> interfaceClass, String operator) throws Exception {
        List<ServiceConfig> serviceList = ProviderParser.getAllServiceList(applicationRegister);
        if(CollectionUtils.isEmpty(serviceList)){
            log.info("No services for this application to disable");
            return;
        }
        if(interfaceClass == null){
            log.debug("InterfaceClass can not be null");
            return;
        }
        log.debug("Start soa service[{}] by operator[{}]", interfaceClass, operator);
        //上送信息
        try {
            for (ServiceConfig config : serviceList) {
                if(config.getInterfaceClass().equals(interfaceClass)){
                    //机器地址：appPath/protocol(thrift/netty)/com.tiger.chaos.xxx_1.0.0(服务名)/providers/127.0.0.0:8081:3
                    String providerPath = ProviderParser.assembleProviderPath(applicationRegister.getAppPath(), config);
                    //创建ephemeral模式的provider地址，并记录service provider信息
                    try {
                        if(zkClient.checkExists().forPath(providerPath) == null){
                            //创建ephemeral模式的provider地址，并记录service provider信息
                            zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
                                    .forPath(providerPath, JSON.toJSONString(config).getBytes(Constants.DEFAULT_CHARSET));
                        }
                        //设置启用标记
                        config.setServiceStatus(ServiceStatus.ENABLED);
                        //处理完成退出循环
                        break;
                    } catch (Exception e) {
                        throw e;
                    }
                }
            }
            log.debug("Soa service[{}] started successfully by operator[{}]", interfaceClass, operator);
        } catch (Exception e) {
            log.error("Provider's[{}/{}] service[{}] enabled error, msg: {}", NetworkUtils.host(), NetworkUtils.ip(),
                    interfaceClass.getSimpleName(), e);
            String msg = String.format("Provider's[%s/%s] services[%s] enabled error", NetworkUtils.host(), NetworkUtils.ip(),
                    interfaceClass.getSimpleName());
            throw new ServiceException(ServiceCodeEnum.SYSTEM_ERROR.getCode(), msg, e);
        }
        log.debug("Soa service[{}] started successfully by operator[{}]", interfaceClass, operator);
        Class<?> enClosedClazz = interfaceClass.getEnclosingClass();
        enClosedClazz = enClosedClazz == null? interfaceClass : enClosedClazz;
        String msg = String.format("【%s】应用【%s】提供者【%s/%s】的服务【%s】被【%s】启用。",
                DateFormatUtils.format(new Date(), Constants.NOTICE_TIME_PATTERN), appName, NetworkUtils.host(), NetworkUtils.ip(),
                enClosedClazz.getName(), operator);
        //告警日志
        log.warn(msg);
        if(isAutoAlertForService && noticeService != null){
            noticeService.notice(owners, NoticeTypeEnum.SERVICE_ENABLED.getDescription(), msg);
        }
        //无异常同步数据
        syncToCustomizedPlaceWithoutException(operator);
    }

    /**
     * 暂停该机器的所有服务，但不暂停服务线程
     * @param operator  操作人
     */
    public void disableCurrentProvider(String operator) throws Exception {
        List<ServiceConfig> serviceList = ProviderParser.getAllServiceList(applicationRegister);
        if(CollectionUtils.isEmpty(serviceList)){
            log.info("No services for this provider to disable");
            return;
        }
        try {
            if(CollectionUtils.isEmpty(serviceList)){
                log.info("No service to unRegister");
                return;
            }
            log.debug("Start to stop provider by operator[{}]", operator);
            //注销所有服务
            for (ServiceConfig config : serviceList) {
                //机器地址：appPath/protocol(thrift/netty)/com.tiger.chaos.xxx_1.0.0(服务名)/providers/127.0.0.0:8081:3
                String providerPath = ProviderParser.assembleProviderPath(applicationRegister.getAppPath(), config);
                try {
                    //注销机器
                    if(zkClient.checkExists().forPath(providerPath) != null){
                        //自动注销时，可以先校验
                        zkClient.delete().forPath(providerPath);
                    }
                    //disable服务
                    config.setServiceStatus(ServiceStatus.DISABLED);
                } catch (Exception e) {
                    log.error("unRegister service[{}] provider[{}] error", config.getInterfaceName(), config.getHost(), e);
                    throw e;
                }
            }
            log.debug("Provider stopped successfully by operator[{}]", operator);
        } catch (Exception e) {
            log.error("Provider disabled error", e);
            List<String> errorList = serviceList.stream().filter(o -> ServiceStatus.ENABLED.equals(o.getServiceStatus()))
                    .map(o -> o.getInterfaceClass().getSimpleName()).collect(Collectors.toList());
            String msg = String.format("Provider's[%s/%s] services[%s] disabled error", NetworkUtils.host(), NetworkUtils.ip(),
                    StringUtils.join(errorList, ","));
            throw new ServiceException(ServiceCodeEnum.SYSTEM_ERROR.getCode(), msg, e);
        }
        String msg = String.format("【%s】应用【%s】的提供者【%s/%s】被【%s】停用。",
                DateFormatUtils.format(new Date(), Constants.NOTICE_TIME_PATTERN), appName, NetworkUtils.host(), NetworkUtils.ip() ,operator);
        //告警日志
        log.warn(msg);
        if(isAutoAlertForService && noticeService != null){
            noticeService.notice(owners, NoticeTypeEnum.PROVIDER_DISABLED.getDescription(), msg);
        }
        //无异常同步数据
        syncToCustomizedPlaceWithoutException(operator);
    }

    /**
     * 暂停该机器的指定服务
     * @param interfaceClass    接口类
     * @param operator  操作人
     * @throws Exception
     */
    public void disableCurrentProviderByService(Class<?> interfaceClass, String operator) throws Exception {
        List<ServiceConfig> serviceList = ProviderParser.getAllServiceList(applicationRegister);
        if(CollectionUtils.isEmpty(serviceList)){
            log.info("No services for this provider to disable");
            return;
        }
        if(interfaceClass == null){
            log.debug("InterfaceClass can not be null");
            return;
        }
        log.debug("Start to stop soa service[{}] by operator[{}]", interfaceClass, operator);
        try {
            for (ServiceConfig config : serviceList) {
                if(config.getInterfaceClass().equals(interfaceClass)){
                    //机器地址：servicePath/providers/127.0.0.0:8081:3
                    String providerPath = ProviderParser.assembleProviderPath(applicationRegister.getAppPath(), config);
                    //删除ephemeral模式的provider地址
                    if(zkClient.checkExists().forPath(providerPath) != null){
                        //临时节点存在，则直接写入信息
                        zkClient.delete().forPath(providerPath);
                    }
                    config.setServiceStatus(ServiceStatus.DISABLED);
                    //处理完成退出循环
                    break;
                }
            }
            log.debug("Soa service[{}] stopped successfully by operator[{}]", interfaceClass, operator);
        } catch (Exception e) {
            log.error("Provider's[{}/{}] service[{}] disabled error, msg: {}", NetworkUtils.host(), NetworkUtils.ip(),
                    interfaceClass.getSimpleName(), e);
            String msg = String.format("Provider's[%s/%s] services[%s] disabled error", NetworkUtils.host(), NetworkUtils.ip(),
                    interfaceClass.getSimpleName());
            throw new ServiceException(ServiceCodeEnum.SYSTEM_ERROR.getCode(), msg, e);
        }
        String msg = String.format("【%s】应用【%s】提供者【%s/%s】的服务被【%s】停用。",
                DateFormatUtils.format(new Date(), Constants.NOTICE_TIME_PATTERN), appName, NetworkUtils.host(), NetworkUtils.ip() ,operator);
        //告警日志
        log.warn(msg);
        if(isAutoAlertForService && noticeService != null){
            noticeService.notice(owners, NoticeTypeEnum.SERVICE_DISABLED.getDescription(), msg);
        }
        //无异常同步数据
        syncToCustomizedPlaceWithoutException(operator);
    }

    /**
     * kill soa服务端
     * @param operator  操作人
     * @throws Exception
     */
    public void killSOAServer(ServiceRegister serviceRegister, String operator) throws Exception {
        log.debug("Start to stop soa server by operator[{}]", operator);
        try {
            //具体服务注销
            serviceRegister.doUnRegister();
            //注销zk服务
            serviceRegister.unRegisterService(ServiceStatus.KILLED);
            log.debug("Soa server stopped successfully by operator[{}]", operator);
        } catch (Exception e) {
            log.error("SOA server[{}/{}] killed error, msg: {}", NetworkUtils.host(), NetworkUtils.ip(), e);
            String msg = String.format("SOA server[%s/%s] services[%s] killed error", NetworkUtils.host(), NetworkUtils.ip());
            throw new ServiceException(ServiceCodeEnum.SYSTEM_ERROR.getCode(), msg, e);
        }
        String msg = String.format("【%s】应用【%s】的提供者【%s/%s】的SOA服务进程被【%s】关闭。",
                DateFormatUtils.format(new Date(), Constants.NOTICE_TIME_PATTERN), appName, NetworkUtils.host(), NetworkUtils.ip() ,operator);
        //告警日志
        log.warn(msg);
        if(isAutoAlertForService && noticeService != null){
            noticeService.notice(owners, String.format(NoticeTypeEnum.PROTOCOL_SERVER_DISABLED.getDescription(), serviceRegister.getProtocolType().getValue()), msg);
        }
        //无异常同步数据
        syncToCustomizedPlaceWithoutException(operator);
    }

    /**
     * start soa单个服务
     * @param operator  操作人
     * @throws Exception
     */
    public void startSOAServer(ServiceRegister serviceRegister, String operator) throws Exception {
        log.debug("Start soa server by operator[{}]", operator);
        try {
            //批量设置killed为可用状态
            for (ServiceConfig config : serviceRegister.getServiceList()) {
                config.setServiceStatus(ServiceStatus.KILLED.equals(config.getServiceStatus())?
                        ServiceStatus.ENABLED : config.getServiceStatus());
            }

            //注册zk服务
            serviceRegister.registerService();
            //具体注册
            serviceRegister.doRegister();

            log.debug("Soa server started successfully by operator[{}]", operator);
        } catch (Exception e){
            log.error("SOA server[{}/{}] started error, msg: {}", NetworkUtils.host(), NetworkUtils.ip(), e);
            String msg = String.format("SOA server[%s/%s] services[%s] started error", NetworkUtils.host(), NetworkUtils.ip());
            throw new ServiceException(ServiceCodeEnum.SYSTEM_ERROR.getCode(), msg, e);
        }
        String msg = String.format("【%s】应用【%s】的提供者【%s/%s】的SOA服务进程被【%s】启用。",
                DateFormatUtils.format(new Date(), Constants.NOTICE_TIME_PATTERN), appName, NetworkUtils.host(), NetworkUtils.ip() ,operator);
        //告警日志
        log.warn(msg);
        if(isAutoAlertForService && noticeService != null){
            noticeService.notice(owners, String.format(NoticeTypeEnum.PROTOCOL_SERVER_ENABLED.getDescription(), serviceRegister.getProtocolType().getValue()), msg);
        }
        //无异常同步数据
        syncToCustomizedPlaceWithoutException(operator);
    }


    /**
     * 上送所有协议服务信息到zk上
     */
    public void upServiceConfigsToZk() throws Exception {
        //遍历服务
        log.debug("Start to sync enabled services config to zk");
        for (ServiceRegister serviceRegister : applicationRegister.getServiceRegisters()) {
            for (ServiceConfig config : serviceRegister.getServiceList()) {
                if(ServiceStatus.ENABLED.equals(config.getServiceStatus())){
                    //机器地址：servicePath/providers/127.0.0.0:8081:3
                    String providerPath = ProviderParser.assembleProviderPath(applicationRegister.getAppPath(), config);
                    try {
                        //创建ephemeral模式的provider地址，并记录service provider信息
                        if(zkClient.checkExists().forPath(providerPath) == null){
                            //不存在，创建
                            zkClient.create().creatingParentsIfNeeded().forPath(providerPath, JSON.toJSONString(config).getBytes(Constants.DEFAULT_CHARSET));
                            continue;
                        }
                        //临时节点存在，则直接写入信息
                        zkClient.setData().forPath(providerPath, JSON.toJSONString(config).getBytes(Constants.DEFAULT_CHARSET));
                    } catch (Exception e) {
                        log.error("Up ServiceConfig[{}] error, ServiceConfig cached, delay do retries automatically, msg: {}", JSON.toJSONString(config), e);
                        throw e;
                    }
                }
            }
        }
        log.debug("Sync enabled services config to zk successfully");
    }

    /**
     * 同步到其他自定义的地方
     * @param syncMachineService
     * @return
     * @throws Exception
     */
    public ServerPacket syncToCustomizedPlace(SyncMachineService syncMachineService) throws Exception {
        return syncToCustomizedPlace(syncMachineService, StringUtils.isNotBlank(appName)? appName : "system");
    }

    /**
     * 同步到其他自定义的地方
     * @param syncMachineService
     * @param operator
     * @return
     * @throws Exception
     */
    public ServerPacket syncToCustomizedPlace(SyncMachineService syncMachineService, String operator) throws Exception {
        //外部自定义同步服务不存在，获取内部同步服务
        SyncMachineService service = syncMachineService == null ? applicationRegister.getSyncMachineService() : syncMachineService;
        if(service == null){
            //如果自定义和外部都不存在，不做定制化同步
            log.warn("No syncMachineService found");
            return null;
        }
        //定制化同步数据
        ServerPacket serverPacket = getLocalServerPacket();
        log.debug("Start to execute customized sync service");
        service.syncServiceMsg(serverPacket, operator);
        log.debug("Execute customized sync service successfully");
        return serverPacket;
    }

    /**
     * 无异常同步数据
     */
    private void syncToCustomizedPlaceWithoutException() {
        //同步信息到定制化地方
        try {
            log.debug("Sync information to the customized place");
            syncToCustomizedPlace(null);
        } catch (Exception e) {
            log.error("Sync information to the customized place error", e);
        }
    }

    /**
     * 无异常同步数据
     * @param operator  同步操作人
     */
    private void syncToCustomizedPlaceWithoutException(String operator) {
        //同步信息到定制化地方
        try {
            log.debug("Sync information to the customized place");
            syncToCustomizedPlace(null, operator);
        } catch (Exception e) {
            log.error("Sync information to the customized place error", e);
        }
    }

    /**
     * 通过机器视角组装本地服务器信息包
     */
    public ServerPacket getLocalServerPacket() {
        ServerPacket serverPacket;

        List<ServiceConfig> serviceConfigList = ProviderParser.getAllServiceList(applicationRegister);

        if(CollectionUtils.isEmpty(serviceConfigList)){
            serverPacket = new ServerPacket();
        } else {
            serverPacket = BeanTransformUtil.transformToServerPacketGroupByHost(serviceConfigList).get(0);
        }
        //设置应用名
        return serverPacket;
    }

}

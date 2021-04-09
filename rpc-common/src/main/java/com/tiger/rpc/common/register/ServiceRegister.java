package com.tiger.rpc.common.register;

import com.alibaba.fastjson.JSON;
import com.tiger.rpc.common.config.MonitorConfig;
import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.common.dto.ServerPacket;
import com.tiger.rpc.common.enums.ProtocolTypeEnum;
import com.tiger.rpc.common.enums.ServiceStatus;
import com.tiger.rpc.common.job.MonitorJob;
import com.tiger.rpc.common.listener.MultiProviderConnectionListener;
import com.tiger.rpc.common.provider.NoticeService;
import com.tiger.rpc.common.provider.SyncMachineService;
import com.tiger.rpc.common.utils.BeanTransformUtil;
import com.tiger.rpc.common.utils.Constants;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.tiger.rpc.common.utils.ProviderParser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.CreateMode;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * @ClassName: ServiceRegister.java
 *
 * @Description: 服务注册
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/19
 */
@Slf4j
public abstract class ServiceRegister implements Closeable {

    /**
     * 应用注册器
     */
    private ApplicationRegister appRegister;

    @Getter
    private int serverPort;

    /**
     * 暴露服务列表
     */
    private List<ServiceConfig> serviceList = new CopyOnWriteArrayList<ServiceConfig>();

    /**
     * 服务名_版本号<--->ServiceConfig
     */
    private Map<String, ServiceConfig> serviceBeanMap = new ConcurrentHashMap<String, ServiceConfig>();

    /**
     * zk监听
     */
    private transient ConnectionStateListener zkConnectionListener;

    /**
     * 是否注册成功标记
     */
    protected volatile boolean isRegistered;

    public ServiceRegister(ApplicationRegister appRegister, List<ServiceConfig> serviceList, int serverPort){
        Preconditions.checkArgument(appRegister != null, "appRegister can not be null");
        this.appRegister = appRegister;
        /**
         * 1.先设置端口
         * 2.在过滤服务
         * 3.将暴露服务注册器回写
         */
        this.serverPort = serverPort;
        doFilterServiceList(serviceList);
        this.appRegister.addServiceRegister(this);
    }

    /**
     * 重连后重新注册，跳过监听（此时已存在监听）
     * @throws Exception
     */
    public void reRegister() throws Exception {
        if (validDependencies()) {
            return;
        }
        //1.注册服务
        registerService(this.serviceList);
        //2.具体注册
        doRegister();
        //3.设置标记
        isRegistered = true;
    }

    /**
     * 注册服务
     */
    public void register() throws Exception {
        log.info("Start to register services...");
        //校验依赖
        if (validDependencies()) {
            return;
        }

        if(appRegister.getNoticeService() == null){
            //不存在时，不告警
            log.warn("NoticeService not found, machine error will not notice owners");
        } else {
            log.debug("NoticeService[{}] found, machine error will notice owners", appRegister.getNoticeService().getClass().getName());
        }

        //1.注册服务
        registerService(this.serviceList);
        //2.具体注册
        doRegister();

        if(this.zkConnectionListener == null){
            //不存在，则使用默认的
            zkConnectionListener = new MultiProviderConnectionListener(this);
        }

        //3.注册zk连接监听
        appRegister.getZkClient().getConnectionStateListenable().addListener(zkConnectionListener);

        //4.设置监控
//        if(monitorJob == null){
//            monitorJob = new MonitorJob(monitorConfig, this);
//            monitorJob.execute();
//        }

        //5.设置注册成功标记
        isRegistered = true;
        log.info("Services registered successfully");
    }

    private boolean validDependencies() throws Exception {
        if(!appRegister.isRegistered()){
            //先注册应用
            log.info("Please register application firstly");
            appRegister.register();
        }
        if(isRegistered){
            //已注册
            log.info("Services has registered");
            return true;
        }
        if(CollectionUtils.isEmpty(serviceList)){
            //无服务注册
            log.info("No services to register");
            return true;
        }
        return false;
    }

    /**
     * 注册zk服务
     * @throws Exception
     */
    public void registerService() throws Exception {
        this.registerService(this.serviceList);
    }

    /**
     * 注册zk服务
     * @param serviceList   服务
     * @throws Exception
     */
    private void registerService(List<ServiceConfig> serviceList) throws Exception {
        List<String> nameServiceList = Lists.newArrayList();
        //注册信息
        Class<?> enClosedClazz;
        for (ServiceConfig config : serviceList) {
            if(ServiceStatus.ENABLED.equals(config.getServiceStatus())) {
                //可用服务注册，不可用服务跳过
                try {
                    enClosedClazz = config.getInterfaceClass().getEnclosingClass();
                    enClosedClazz = enClosedClazz == null? config.getInterfaceClass() : enClosedClazz;
                    nameServiceList.add( enClosedClazz.getName() + Constants.SERVICE_VERSION_SEPARATOR
                            + config.getVersion());
                    //机器地址：appPath/tprotocol(thrift/netty)/com.tiger.chaos.xxx_1.0.0(服务名)/providers/127.0.0.0:8081:3
                    String providerPath = ProviderParser.assembleProviderPath(this.getAppRegister().getAppPath(), config);
                    //创建ephemeral模式的provider地址，并记录service provider信息
                    if(appRegister.getZkClient().checkExists().forPath(providerPath) == null){
                        //节点不存在创建，并写入数据
                        appRegister.getZkClient().create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
                                .forPath(providerPath, JSON.toJSONString(config).getBytes(Constants.DEFAULT_CHARSET));
                        continue;
                    }
                    //节点存在，直接写入数据
                    appRegister.getZkClient().setData().forPath(providerPath, JSON.toJSONString(config).getBytes(Constants.DEFAULT_CHARSET));
                } catch (Exception e) {
                    log.error("register service[{}] provider[{}] error", config.getInterfaceName(), config.getHost(), e);
                    nameServiceList.clear();
                    throw e;
                }
            }
        }
        //打印服务注册日志
        log.debug("ServiceList[{}] registered to zookeeper", StringUtils.join(nameServiceList, ","));
    }

    /**
     * 注销provider和服务
     * @throws Exception
     */
    public void unRegister() throws Exception {
        if(!isRegistered){
            //已注销不处理
            return;
        }
        //具体服务注销
        doUnRegister();
        //注销zk服务
        unRegisterService(ServiceStatus.DISABLED);

        //同步信息上送
        try {
            if(appRegister.getSyncMachineService() != null){
                ServerPacket serverPacket = CollectionUtils.isEmpty(this.serviceList)? new ServerPacket() :
                        BeanTransformUtil.transformToServerPacketGroupByHost(this.serviceList).get(0);
                appRegister.getSyncMachineService().syncServiceMsg(serverPacket, this.appRegister.getAppConf().getName());
            }
        } catch (Exception e) {
            log.warn("Sync unRegister information error.", e);
        }
    }

    /**
     * 注销zk服务: 不传时默认disable处理，只有明确kill时才最处理
     * @param status    状态：null or DISABLED or KILLED
     * @throws Exception
     */
    public void unRegisterService(ServiceStatus status) throws Exception {
        if(CollectionUtils.isEmpty(serviceList)){
            log.info("No service to unRegister");
            return;
        }
        List<String> nameServiceList = Lists.newArrayList();
        status = status == null || status.equals(ServiceStatus.DISABLED) ? ServiceStatus.DISABLED : ServiceStatus.KILLED;
        for (ServiceConfig config : serviceList) {
            //可用服务注册，不可用服务跳过
            if(ServiceStatus.ENABLED.equals(config.getServiceStatus())) {
                try {
                    Class<?> enClosedClazz = config.getInterfaceClass().getEnclosingClass();
                    enClosedClazz = enClosedClazz == null ? config.getInterfaceClass() : enClosedClazz;
                    nameServiceList.add( enClosedClazz.getName() + Constants.SERVICE_VERSION_SEPARATOR + config.getVersion());

                    //机器地址：appPath/protocol(thrift/netty)/com.tiger.chaos.xxx_1.0.0(服务名)/providers/127.0.0.0:8081:3
                    String providerPath = ProviderParser.assembleProviderPath(appRegister.getAppPath(), config);
                    //注销机器，可以先校验
                    if(appRegister.getZkClient().checkExists().forPath(providerPath) != null){
                        appRegister.getZkClient().delete().forPath(providerPath);
                    }
                    //disable服务
                    config.setServiceStatus(status);
                } catch (Exception e) {
                    log.error("unRegister service[{}] provider[{}] error", config.getInterfaceName(), config.getHost(), e);
                    nameServiceList.clear();
                    throw e;
                }
            }
        }

        //设置未注册
        isRegistered = false;
        //打印服务注销日志
        log.debug("ServiceList[{}] removed from zookeeper", StringUtils.join(nameServiceList, ","));
    }

    /**
     * 具体服务注销操作
     * @throws Exception
     */
    public abstract void doUnRegister() throws Exception;

    public ApplicationRegister getAppRegister() {
        return appRegister;
    }

    public ServiceRegister setAppRegister(ApplicationRegister appRegister) {
        Preconditions.checkArgument(appRegister != null, "appRegister can not be null");
        Preconditions.checkArgument(appRegister.isRegistered(), "Application must be registered before services");
        //将暴露服务注册器回写
        appRegister.addServiceRegister(this);
        this.appRegister = appRegister;
        return this;
    }

    public <T extends ServiceRegister> T importServiceList(List<ServiceConfig> serviceList) throws Exception {
        //1.过滤服务
        List<ServiceConfig> protocolConfigs = doFilterServiceList(serviceList);
        if(CollectionUtils.isNotEmpty(protocolConfigs) && this.isRegistered){
            //存在协议服务并且已经加入serviceBeanMap了 & 该服务注册器已经注册了
            //2.注册consumer
            registerService(protocolConfigs);

            //3.具体发现服务
            doRegisterServices(protocolConfigs);
        }
        return (T) this;
    }

    /**
     * 注册服务列表
     * @param serviceList
     * @throws Exception
     */
    protected abstract void doRegisterServices(List<ServiceConfig> serviceList) throws Exception;

    public List<ServiceConfig> getServiceList() {
        return serviceList;
    }

    public ServiceRegister setServiceList(List<ServiceConfig> serviceList) {
        //额外处理
        doFilterServiceList(serviceList);
        return this;
    }

    /**
     * 获取bean map
     * @return
     */
    public Map<String, ServiceConfig> getServiceBeanMap() {
        return serviceBeanMap;
    }

    /**
     * 获取服务接口名称列表
     * @return
     */
    public List<String> getInterfaceNameList(){
        return this.serviceList.stream().map(o -> o.getInterfaceName()).collect(Collectors.toList());
    }

    /**
     * 设置连接监听器
     * @param zkConnectionListener
     * @return
     */
    public ServiceRegister setZkConnectionListener(ConnectionStateListener zkConnectionListener) {
        this.zkConnectionListener = zkConnectionListener;
        return this;
    }

    /**
     * 过滤出协议的服务
     * @param serviceList   服务列表
     * @return
     */
    protected List<ServiceConfig> doFilterServiceList(List<ServiceConfig> serviceList){
        List<ServiceConfig> protocolConfigs  = Lists.newArrayList();
        if(CollectionUtils.isNotEmpty(serviceList)){
            //存在的话，只需要对应protocol协议服务
            serviceList.stream().forEach(o -> {
                if(getProtocolType().getValue().equals(o.getProtocol())){
                    //设置端口为当前服务注册器端口
                    o.setPort(this.serverPort);
                    //组成map，过滤出protocol协议服务，进行注册
                    Class<?> enClosedClazz = o.getInterfaceClass().getEnclosingClass();
                    enClosedClazz = enClosedClazz == null? o.getInterfaceClass() : enClosedClazz;
                    if(null == getServiceBeanMap().putIfAbsent(enClosedClazz.getName() + "" + o.getVersion(), o)){
                        //不存在时，加入服务列表和临时缓冲中；已存在跳过
                        getServiceList().add(o);
                        protocolConfigs.add(o);
                    }
                }
            });
        }
        return protocolConfigs;
    }

    /**
     * 获取协议类型
     * @return
     */
    public abstract ProtocolTypeEnum getProtocolType();

    /**
     * 注册具体的协议服务，返回服务线程
     * @return
     * @throws Exception
     */
    public abstract void doRegister() throws Exception;

    /**
     * 协议服务线程是否服务
     * @return
     */
    public abstract boolean isProtocolServiceServing();

    /**
     * 获取服务名称列表(含版本号)
     * @return
     */
    public List<String> getServiceNameList(){
        return new ArrayList<>(serviceBeanMap.keySet());
    }

    public boolean isRegistered() {
        return isRegistered;
    }

}

package com.tiger.rpc.common.register;

import com.alibaba.fastjson.JSON;
import com.tiger.rpc.common.config.ApplicationConfig;
import com.tiger.rpc.common.config.MonitorConfig;
import com.tiger.rpc.common.config.ZkConfig;
import com.tiger.rpc.common.job.MonitorJob;
import com.tiger.rpc.common.provider.NoticeService;
import com.tiger.rpc.common.provider.SyncMachineService;
import com.tiger.rpc.common.utils.Constants;
import com.tiger.rpc.common.utils.ZkUtils;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.CreateMode;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @ClassName: ApplicationRegister.java
 *
 * @Description: 应用注册
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/19
 */
@Slf4j
public class ApplicationRegister implements Closeable {

    /**
     * zkClient不参与序列化
     */
    private transient CuratorFramework zkClient;

    /**
     * 应用注册路径
     */
    private String appPath;

    /**
     * 应用配置
     */
    private ApplicationConfig appConf;

    /**
     * 引入注册器列表
     */
    private Set<ReferenceRegister> referenceRegisters = Sets.newConcurrentHashSet();

    /**
     * 服务注册器列表
     */
    private Set<ServiceRegister> serviceRegisters = Sets.newConcurrentHashSet();

    /**
     * 代表应用是否注册过
     */
    private transient volatile boolean isRegistered;

    /**
     * zk配置
     */
    private ZkConfig zkConfig;

    /**
     * 监控配置，未传入使用默认值
     */
    private MonitorConfig monitorConfig = new MonitorConfig();

    /**
     * 应用监控job
     */
    private MonitorJob monitorJob;

    /**
     * 通知服务
     */
    private NoticeService noticeService;

    /**
     * 机器信息同步服务
     */
    private SyncMachineService syncMachineService;

    public ApplicationRegister(ZkConfig zkConfig, ApplicationConfig appConf) {
        this(zkConfig, null, appConf, null);
    }

    public ApplicationRegister(ZkConfig zkConfig, CuratorFramework zkClient, ApplicationConfig appConf) {
        this(zkConfig, zkClient, appConf, null);
    }

    public ApplicationRegister(ZkConfig zkConfig, CuratorFramework zkClient, ApplicationConfig appConf, SyncMachineService syncMachineService) {
        this(zkConfig, zkClient, appConf, null, null, syncMachineService);
    }

    public ApplicationRegister(ZkConfig zkConfig, CuratorFramework zkClient, ApplicationConfig appConf, NoticeService noticeService, SyncMachineService syncMachineService) {
        this(zkConfig, zkClient, appConf, null, noticeService, syncMachineService);
    }

    public ApplicationRegister(ZkConfig zkConfig, CuratorFramework zkClient, ApplicationConfig appConf, MonitorConfig monitorConfig, NoticeService noticeService, SyncMachineService syncMachineService) {
        if (zkClient != null && "".equalsIgnoreCase(zkClient.getNamespace())) {
            //传入无namespace的zkClient时，直接使用外部的，兼容多应用使用同一个的zkClient，避免资源浪费
            this.zkClient = zkClient;
        } else {
            //未传入zkClient时，创建不含namespace的zk客户端
            this.zkClient = ZkUtils.createZkClientWithoutNameSpace(zkConfig);
        }
        this.zkConfig = zkConfig;
        //校验应用配置
        validApplicationConf(appConf);
        this.setAppConf(appConf);
        this.monitorConfig = monitorConfig;
        this.noticeService = noticeService;
        this.syncMachineService = syncMachineService;
        if (this.monitorConfig != null) {
            //如果存在监控配置时，则会创建监控器，并启动监控器
            this.monitorJob = new MonitorJob(this.monitorConfig, this);
            this.monitorJob.execute();
        }
    }

    private void validApplicationConf(ApplicationConfig applicationConf){
        if(StringUtils.isBlank(applicationConf.getName())){
            //应用名不可为空
            throw new IllegalArgumentException("name can not be null");
        }
        if(StringUtils.isBlank(applicationConf.getOwner())){
            //应用负责人不可为空
            throw new IllegalArgumentException("owner can not be null");
        }
        if(StringUtils.isBlank(applicationConf.getGroup())){
            //部门不可为空
            throw new IllegalArgumentException("group can not be null");
        }
        if(StringUtils.isBlank(applicationConf.getEnv())){
            //环境不可为空
            throw new IllegalArgumentException("env can not be null");
        }
    }

    /**
     * 注册应用
     */
    public void register() throws Exception {
        try {
            if(isRegistered){
                log.info("The application has registered");
                return;
            }
            //不存在时创建应用节点
            if(zkClient.checkExists().forPath(appPath) == null){
                //创建应用的永久节点
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(appPath);
                //应用节点中记录引用的注册信息
                zkClient.setData().forPath(appPath, JSON.toJSONString(appConf).getBytes(Constants.DEFAULT_CHARSET));
            }
            isRegistered = true;
        } catch (Exception e){
            log.error("Register application[{}] error", appConf.getName(), e);
            throw e;
        }
    }

    /**
     * 注销应用：1.consumer/provider注销
     *          2.application注销
     *          3.此在实例销毁前注销资源
     * @throws Exception
     */
    public void unRegister() throws Exception {
        try{
            if(!isRegistered){
                log.info("The application is not registered");
                return;
            }
            //暴露服务注册器注销
            for (ServiceRegister register: serviceRegisters) {
                if(register != null && register.isRegistered){
                    register.unRegister();
                }
            }

            //引入注册器注销
            for (ReferenceRegister register: referenceRegisters) {
                if(register != null && register.isRegistered){
                    register.unDiscovery();
                }
            }
            if(CollectionUtils.isEmpty(zkClient.getChildren().forPath(appPath))){
                //应用废弃时，删除应用节点
                zkClient.delete().forPath(appPath);
            }
            isRegistered = false;
        } catch (Exception e){
            log.error("Unregister application[{}] error", e);
            throw e;
        }
    }

    public CuratorFramework getZkClient() {
        if(!zkClient.getState().equals(CuratorFrameworkState.STARTED)){
            try {
                zkClient.start();
                zkClient.wait(100L);
            } catch (InterruptedException e) {
                log.error("zkClient start error", e);
            }
        }
        return zkClient;
    }

    public String getAppPath() {
        return appPath;
    }

    public ApplicationConfig getAppConf() {
        return appConf;
    }

    public ApplicationRegister setAppConf(ApplicationConfig appConf) {
        validApplicationConf(appConf);
        this.appConf = appConf;
        //路径：/soa/环境/组织/应用_版本
        StringBuffer sb = new StringBuffer(Constants.ROOT_PATH);
        sb.append(Constants.PATH_SEPARATOR).append(appConf.getEnv()).append(Constants.PATH_SEPARATOR)
                .append(appConf.getGroup()).append(Constants.PATH_SEPARATOR).append(appConf.getName())
                .append(Constants.APPLICATION_VERSION_SEPARATOR).append(appConf.getVersion());
        appPath = sb.toString();
        return this;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void addReferenceRegister(ReferenceRegister referenceRegister) {
        if(referenceRegister != null){
            this.referenceRegisters.add(referenceRegister);
        }
    }

    public void addServiceRegister(ServiceRegister serviceRegister) {
        if(serviceRegister != null){
            this.serviceRegisters.add(serviceRegister);
        }
    }

    /**
     * 获取所有服务注册器
     * @return
     */
    public Set<ServiceRegister> getServiceRegisters(){
        //不暴露原有的列表，防止被篡改
        if (CollectionUtils.isEmpty(this.serviceRegisters)) {
            return new HashSet<>();
        }
        return new HashSet<>(this.serviceRegisters);
    }

    public ZkConfig getZkConfig() {
        return zkConfig;
    }

    /**
     * 设置zk配置：
     *      1.校验配置
     *      2.zkClient不存在时，创建
     * @param zkConfig
     */
    public ApplicationRegister setZkConfig(ZkConfig zkConfig) {
        ZkUtils.validateZkConfig(zkConfig);
        this.zkConfig = zkConfig;
        //创建不含namespace的zk客户端
        if(this.zkClient == null){
            this.zkClient = ZkUtils.createZkClientWithoutNameSpace(zkConfig);
        }
        return this;
    }

    public MonitorConfig getMonitorConfig() {
        return monitorConfig;
    }

    public ApplicationRegister setMonitorConfig(MonitorConfig monitorConfig) {
        this.monitorConfig = monitorConfig;
        return this;
    }

    public MonitorJob getMonitorJob() {
        return monitorJob;
    }

    public ApplicationRegister setMonitorJob(MonitorJob monitorJob) {
        this.monitorJob = monitorJob;
        return this;
    }

    public NoticeService getNoticeService() {
        return noticeService;
    }

    public ApplicationRegister setNoticeService(NoticeService noticeService) {
        this.noticeService = noticeService;
        return this;
    }

    public SyncMachineService getSyncMachineService() {
        return syncMachineService;
    }

    public ApplicationRegister setSyncMachineService(SyncMachineService syncMachineService) {
        this.syncMachineService = syncMachineService;
        return this;
    }

    @Override
    public void close() throws IOException {
        try {
//            this.unRegister();
        } catch (Exception e) {
            log.error("Close ApplicationRegister error", e);
        }
    }

}

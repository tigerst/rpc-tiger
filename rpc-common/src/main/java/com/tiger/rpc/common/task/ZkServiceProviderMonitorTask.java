package com.tiger.rpc.common.task;

import com.alibaba.fastjson.JSON;
import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.common.config.ZkConfig;
import com.tiger.rpc.common.enums.NoticeTypeEnum;
import com.tiger.rpc.common.enums.ServiceStatus;
import com.tiger.rpc.common.provider.NoticeService;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.common.register.ServiceRegister;
import com.tiger.rpc.common.utils.Constants;
import com.tiger.rpc.common.utils.NetworkUtils;
import com.tiger.rpc.common.utils.ProviderParser;
import com.tiger.rpc.common.utils.ZkUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: ZkProtocolServiceProviderMonitorTask.java
 *
 * @Description: zk上服务提供者监控task：task 5min~10min监控一次
 *
 * @Author: Tiger
 *
 * @Date: 2019/4/28
 */
@Slf4j
public class ZkServiceProviderMonitorTask implements Runnable {

    /**
     * 通知服务
     */
    private NoticeService noticeService;

    /**
     * 应用注册器
     */
    private ApplicationRegister appRegister;

    /**
     * zk配置
     */
    private ZkConfig zkConfig;

    /**
     * 阈值，默认为2
     */
    private volatile int threshold = 2;

    /**
     * owner列表
     */
    private List<String> owners = Lists.newArrayList();

    /**
     * 集群
     */
    private String cluster;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * zkClient
     */
    private CuratorFramework zkClient;

    public ZkServiceProviderMonitorTask(ApplicationRegister appRegister){
        this(appRegister, 2);
    }

    public ZkServiceProviderMonitorTask(ApplicationRegister appRegister, int threshold){
        ZkUtils.validateZkConfig(appRegister.getZkConfig());
        this.zkConfig = appRegister.getZkConfig();
        //分割处理owner
        this.owners.addAll(Arrays.asList(appRegister.getAppConf().getOwner().split(Constants.OWNER_SEPARATOR)));
        //获取集群名称
        this.cluster = appRegister.getAppConf().getCluster();
        //获取应用名称
        this.appName = appRegister.getAppConf().getName();
        this.appRegister = appRegister;
        this.noticeService = appRegister.getNoticeService();
        this.threshold = threshold;
    }


    @Override
    public void run() {
        log.debug("Start to execute " + this.getClass().getSimpleName());
        boolean isMaster = false;
        try {
            //获取注册的服务
            List<ServiceConfig> serviceList = ProviderParser.getAllServiceList(appRegister);
            //获取zkClient
            isMaster = isMaster(getZkClient());
            if(!isMaster){
                //非master跳过
                log.debug("The machine[{}/{}] is not master", NetworkUtils.host(), NetworkUtils.ip());
                return;
            }
            String providers;
            int curSize;
            for (ServiceConfig config : serviceList) {
                Class<?> enClosedClazz = config.getInterfaceClass().getEnclosingClass();
                enClosedClazz = enClosedClazz == null? config.getInterfaceClass() : enClosedClazz;
                if(ServiceStatus.DISABLED.equals(config.getServiceStatus()) || ServiceStatus.KILLED.equals(config.getServiceStatus())){
                    //打印日志
                    log.debug("Current application[{}] service[{}] {}", appName, config, enClosedClazz.getSimpleName(),
                            config.getServiceStatus().toString().toLowerCase());
                    continue;
                }
                try {
                    providers = ProviderParser.assembleServicePath(appRegister.getAppPath(), config) + Constants.PATH_SEPARATOR + "providers";
                    //获取zk上服务提供者
                    curSize = zkClient.getChildren().forPath(providers).size();
                    if(curSize < threshold){
                        //小于阈值时，告警
                        String msg = String.format("【%s】集群【%s】应用【%s】的服务【%s】提供者数量【%d】小于阈值【%d】，请检查zk路径【%s】。",
                                DateFormatUtils.format(new Date(), Constants.NOTICE_TIME_PATTERN),
                                cluster, appName, enClosedClazz.getSimpleName(), curSize, threshold, providers);
                        //告警日志
                        log.warn(msg);
                        try {
                            if(noticeService != null){
                                //执行服务异常通知
                                noticeService.notice(this.owners, NoticeTypeEnum.PROVIDER_ABNORMAL.getDescription(), msg);
                            }
                        } catch (Exception e1){
                            log.error("Execute noticeService[" + noticeService == null ? null : noticeService.getClass() + "] error", e1);
                        }
                    } else {
                        //打印当前日志
                        log.debug("Current application[{}] service[{}] provider's size[{}]", appName, enClosedClazz.getSimpleName(), curSize);
                    }
                } catch (Exception e) {
                    log.error("Check config[{}]  error, msg: {}", JSON.toJSONString(config), e);
                }
            }
        } catch (Exception e){
            log.error("Execute " +  NoticeTypeEnum.PROVIDER_ABNORMAL.getDescription() + " error", e);
        } finally {
            releaseMaster(zkClient, isMaster);
        }
        log.debug("End execute " + this.getClass().getSimpleName());
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    /**
     * master选举
     * @return
     * @throws Exception
     * @param zkClient
     */
    private boolean isMaster(CuratorFramework zkClient) throws Exception {
        try {
            //选举路径
            StringBuffer sb = new StringBuffer();
            sb.append(Constants.PATH_SEPARATOR).append(zkConfig.getNamespace()).append(Constants.PATH_SEPARATOR)
                    .append(appName).append(Constants.PATH_SEPARATOR).append("monitor").append(Constants.PATH_SEPARATOR)
                    .append(this.getClass().getSimpleName());
            //创建选举路径的临时节点，如果创建成功，则为master，否则跳过
            zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
                    .forPath(sb.toString(), NetworkUtils.host().getBytes(Constants.DEFAULT_CHARSET));
            log.debug("Monitor master is current machine[{}]", NetworkUtils.host());
            return true;
        } catch (KeeperException.NodeExistsException e) {
            log.debug("Monitor master exists", e);
            //节点存在，跳出
            return false;
        }
    }

    /**
     * 回收资源
     * @param zkClient
     * @param isMaster
     */
    private void releaseMaster(CuratorFramework zkClient, boolean isMaster){
        try {
            if(zkClient != null){
                if(zkClient.getState() == CuratorFrameworkState.STARTED){
                    //启动状态，则移除路径
                    //选举路径
                    StringBuffer sb = new StringBuffer();
                    sb.append(Constants.PATH_SEPARATOR).append(zkConfig.getNamespace()).append(Constants.PATH_SEPARATOR)
                            .append(appName).append(Constants.PATH_SEPARATOR).append("monitor").append(Constants.PATH_SEPARATOR)
                            .append(this.getClass().getSimpleName());
                    if(isMaster && zkClient.checkExists().forPath(sb.toString()) != null){
                        //master 且 节点存在
                        zkClient.delete().forPath(sb.toString());
                        log.debug("Delete zk path[{}] successfully", sb.toString());
                        log.debug("Release master successfully");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Release master error", e);
        }
    }

    /**
     * 获取zkClient
     * @return
     */
    public CuratorFramework getZkClient() {
        if(this.zkClient == null || this.zkClient.getState() == CuratorFrameworkState.STOPPED){
            zkClient = ZkUtils.createZkClientWithoutNameSpace(zkConfig);
        }
        log.debug("get zkClient successfully");
        return zkClient;
    }
}

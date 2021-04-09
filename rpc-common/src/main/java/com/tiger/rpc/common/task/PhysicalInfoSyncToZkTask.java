package com.tiger.rpc.common.task;

import com.tiger.rpc.common.config.MonitorConfig;
import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.common.dto.MachinePacket;
import com.tiger.rpc.common.dto.ServerPacket;
import com.tiger.rpc.common.enums.NoticeTypeEnum;
import com.tiger.rpc.common.helper.ServiceHelper;
import com.tiger.rpc.common.provider.NoticeService;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.common.utils.Constants;
import com.tiger.rpc.common.utils.ProviderParser;
import com.tiger.rpc.common.utils.VMStat;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: PhysicalInfoSyncToZkTask.java
 *
 * @Description: 同步机器信息到zk的task，资源不足时，告警
 *
 * @Author: Tiger
 *
 * @Date: 2019/4/25
 */
@Slf4j
public class PhysicalInfoSyncToZkTask implements Runnable {

    /**
     * 服务端工具
     */
    private ServiceHelper serviceHelper;

    /**
     * 是否自动告警资源
     */
    private boolean isAutoAlertForResource;

    /**
     * 通知接口
     */
    private NoticeService noticeService;

    /**
     * 监控配置
     */
    private MonitorConfig monitorConfig;

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

    private ApplicationRegister appRegister;

    public PhysicalInfoSyncToZkTask(ApplicationRegister appRegister){
        this.serviceHelper = new ServiceHelper(appRegister);
        this.isAutoAlertForResource = appRegister.getMonitorConfig().isAutoAlertForResource();
        this.noticeService = appRegister.getNoticeService();
        this.monitorConfig = appRegister.getMonitorConfig();
        this.owners.addAll(Arrays.asList(appRegister.getAppConf().getOwner().split(Constants.OWNER_SEPARATOR)));
        //获取集群名称
        this.cluster = appRegister.getAppConf().getCluster();
        //获取应用名称
        this.appName = appRegister.getAppConf().getName();
        this.appRegister = appRegister;
    }

    @Override
    public void run() {
        try {
            log.debug("Start to up physical Information to zookeeper");
            List<ServiceConfig> serviceList = ProviderParser.getAllServiceList(appRegister);
            if(CollectionUtils.isEmpty(serviceList)){
                //无服务时，跳过
                log.debug("No service Found");
                return;
            }
            for (ServiceConfig config : serviceList) {
                //设置处理器最大可用数
                config.setAvailableProcessors(VMStat.getProcessNum());
                //设置瞬时cpu使用率
                config.setCpuUsage(VMStat.getInstantCpuUsage());
                //设置内存使用率
                config.setMemUsage(VMStat.getMemUsage());
                //设置磁盘使用率
                config.setDiskUsage(VMStat.getDiskUsage());
            }
            //上送配置到zookeeper上
            serviceHelper.upServiceConfigsToZk();
            //同步到默认定制化地方
            ServerPacket serverPacket = serviceHelper.syncToCustomizedPlace(null);
            if(serverPacket == null){
                //不存在，则跳过
                return;
            }
            //处理告警
            alert(serverPacket);
            log.debug("Up physical Information to zookeeper successfully");
        }catch (Exception e) {
            log.error("Up physical Information to zookeeper error", e);
        }
    }

    /**
     * 资源告警
     * @param serverPacket  服务器包
     * @throws Exception
     */
    private void alert(ServerPacket serverPacket) throws Exception {
        MachinePacket machinePacket = serverPacket.getMachinePacket();
        double cpuUsage = machinePacket.getCpuUsage();
        double memUsage = machinePacket.getMemUsage();
        double diskUsage = machinePacket.getDiskUsage();
        //资源情况与阈值比较：cpu使用率、内存使用率和磁盘使用率
        if(cpuUsage >= monitorConfig.getCpuUsageThreshold()
                || memUsage >= monitorConfig.getMemUsageThreshold()
                || diskUsage >= monitorConfig.getDiskUsageThreshold()) {
            String msg =  String.format("【%s】集群【%s】应用【%s】机器【%s/%s】资源不足，处理器使用率【%s】，内存使用率【%s】，磁盘使用率【%s】。",
                    DateFormatUtils.format(new Date(), Constants.NOTICE_TIME_PATTERN), cluster, appName,
                    machinePacket.getHostName(), machinePacket.getIp(), cpuUsage, memUsage, diskUsage);
            //告警日志
            log.warn(msg);
            //执行服务异常通知
            if(noticeService != null && isAutoAlertForResource){
                log.info("Start to notice alert[{}]", NoticeTypeEnum.INSUFFICIENT_RESOURCE.getCode());
                noticeService.notice(this.owners, NoticeTypeEnum.INSUFFICIENT_RESOURCE.getDescription(), msg);
                log.info("Execute alert[{}] successfully", NoticeTypeEnum.INSUFFICIENT_RESOURCE.getCode());
            }
        } else {
            log.debug("Current machine[{}/{}] information: cpuUsage[{}] memUsage[{}] diskUsage[{}]", machinePacket.getHostName(), machinePacket.getIp(),
                    cpuUsage, memUsage, diskUsage);
        }
    }


}

package com.tiger.rpc.common.task;


import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.common.enums.NoticeTypeEnum;
import com.tiger.rpc.common.enums.ProtocolTypeEnum;
import com.tiger.rpc.common.enums.ServiceStatus;
import com.tiger.rpc.common.provider.NoticeService;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.common.register.ServiceRegister;
import com.tiger.rpc.common.utils.Constants;
import com.tiger.rpc.common.utils.NetworkUtils;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @ClassName: ProtocolServiceMonitorTask.java
 *
 * @Description: protocol服务监控（thrift/netty监控）task 5min~10min监控一次
 *
 * @Author: Tiger
 *
 * @Date: 2019/4/28
 */
@Slf4j
public class ServiceMonitorTask implements Runnable {

    /**
     * 通知服务
     */
    private NoticeService noticeService;

    /**
     * 是否自动处理：为true时，自动调起线程，并发送通知；为false时，只发通知
     */
    private volatile boolean isAutoProcess;

    /**
     * 服务注册器
     */
    private ApplicationRegister appRegister;

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

    public ServiceMonitorTask(boolean isAutoProcess, ApplicationRegister appRegister){
        this.noticeService = appRegister.getNoticeService();
        this.isAutoProcess = isAutoProcess;
        this.appRegister = appRegister;
        //分割处理owner
        this.owners.addAll(Arrays.asList(appRegister.getAppConf().getOwner().split(Constants.OWNER_SEPARATOR)));
        //获取集群名称
        this.cluster = appRegister.getAppConf().getCluster();
        //获取应用名称
        this.appName = appRegister.getAppConf().getName();
    }

    @Override
    public void run() {

        if (appRegister == null) {
            log.warn("No application found.");
            return;
        }
        Set<ServiceRegister> serviceRegisters =  appRegister.getServiceRegisters();
        if (CollectionUtils.isEmpty(serviceRegisters)) {
            log.warn("No service register found.");
            return;
        }
        for (ServiceRegister register: serviceRegisters) {
            log.debug("start to check protocol[{}] service", register.getProtocolType().getValue());
            try {
                //检测服务情况
                boolean serviceServing = register.isProtocolServiceServing();
                if(isAutoProcess && !serviceServing){
                    //自动处理 & 异常：批量设置killed为可用状态、注册zk服务、具体注册
                    for (ServiceConfig config : register.getServiceList()) {
                        config.setServiceStatus(ServiceStatus.KILLED.equals(config.getServiceStatus())?
                                ServiceStatus.ENABLED : config.getServiceStatus());
                    }
                    register.registerService();
                    register.doRegister();

                    String msg = String.format("【%s】机器【%s/%s】的%s服务异常【已自动处理】，请检查集群【%s】应用【%s】。",
                            DateFormatUtils.format(new Date(), Constants.NOTICE_TIME_PATTERN), NetworkUtils.host(), NetworkUtils.ip(),
                            register.getProtocolType().getValue(), cluster, appName);
                    //告警日志
                    log.warn(msg);
                    if(noticeService != null){
                        //执行服务异常通知
                        noticeService.notice(owners, String.format(NoticeTypeEnum.PROTOCOL_SERVICE_ABNORMAL.getDescription(),
                                register.getProtocolType().getValue()), msg);
                    }
                } else if(!serviceServing){
                    //自动处理 & 异常：只执行服务异常通知
                    String msg = String.format("【%s】机器【%s/%s】的%s服务异常，请检查集群【%s】应用【%s】。",
                            DateFormatUtils.format(new Date(), Constants.NOTICE_TIME_PATTERN), NetworkUtils.host(), NetworkUtils.ip(),
                            register.getProtocolType().getValue(), cluster, appName);
                    //告警日志
                    log.warn(msg);
                    if(noticeService != null){
                        noticeService.notice(owners, String.format(NoticeTypeEnum.PROTOCOL_SERVICE_ABNORMAL.getDescription(),
                                register.getProtocolType().getValue()), msg);
                    }
                } else {
                    //打印正常日志
                    log.debug("Protocol service[{}] is normal", register.getProtocolType().getValue());
                }
            } catch (Exception e) {
                log.error(new StringBuilder().append("Check protocol[").append("protocolTypeEnum.getValue()").append("] service error").toString(), e);
            }
            log.debug("End check protocol[{}] service", register.getProtocolType().getValue());
        }
    }


    public boolean isAutoProcess() {
        return isAutoProcess;
    }

    public void setAutoProcess(boolean autoProcess) {
        isAutoProcess = autoProcess;
    }

}

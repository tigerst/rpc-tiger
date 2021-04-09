package com.tiger.rpc.common.job;

import com.tiger.rpc.common.config.MonitorConfig;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.common.task.PhysicalInfoSyncToZkTask;
import com.tiger.rpc.common.task.ServiceMonitorTask;
import com.tiger.rpc.common.task.ZkServiceProviderMonitorTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: MonitorJob.java
 *
 * @Description: 应用监控job：监控protocol(thrift/netty)服务，zk上的provider，机器信息监控（上送数据），JMX上送
 *
 * @Author: Tiger
 *
 * @Date: 2019/4/28
 */
@Slf4j
public class MonitorJob {

    /**
     * 同步线程
     */
    private ScheduledExecutorService syncJob;

    /**
     * 监控线程池
     */
    private ScheduledExecutorService monitorJob;

    /**
     * 监控配置
     */
    private MonitorConfig monitorConfig;

    /**
     * 应用注册器
     */
    private ApplicationRegister register;

    public MonitorJob(MonitorConfig monitorConfig, ApplicationRegister register){
        //监控配置
        syncJob = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat(this.getClass().getSimpleName() + "_sync" + "-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler((t, e) -> {
                    log.error("Unexpected exception in thread: " + t, e);
                    throw new RuntimeException(e);
                })
                .build());
        //监控配置
        monitorJob = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setNameFormat(this.getClass().getSimpleName() + "_monitor" + "-%d")
                .setDaemon(true)
                .setUncaughtExceptionHandler((t, e) -> {
                    log.error("Unexpected exception in thread: " + t, e);
                    throw new RuntimeException(e);
                })
                .build());

        this.monitorConfig = monitorConfig;
        this.register = register;
    }

    /**
     * 执行job
     */
    public void execute(){
        //加入机器定时同步-每台机器执行
        syncJob.scheduleWithFixedDelay(new PhysicalInfoSyncToZkTask(register),
                monitorConfig.getSyncInitialDelay(), monitorConfig.getSyncDelay(),
                TimeUnit.valueOf(monitorConfig.getSyncTimeUnit()));

        //加入服务定时监控-每天机器执行
        monitorJob.scheduleWithFixedDelay(new ServiceMonitorTask(monitorConfig.isAutoProcessServiceThread(), register),
                monitorConfig.getServiceMonitorInitialDelay(), monitorConfig.getServiceMonitorDelay(),
                TimeUnit.valueOf(monitorConfig.getServiceMonitorTimeUnit()));

        //加入zk服务定时监控-master执行
        monitorJob.scheduleWithFixedDelay(new ZkServiceProviderMonitorTask(register, monitorConfig.getProvidersThreshold()),
                monitorConfig.getZkProviderMonitorInitialDelay(), monitorConfig.getZkProviderMonitorDelay(),
                TimeUnit.valueOf(monitorConfig.getZkProviderMonitorTimeUnit()));

    }


}

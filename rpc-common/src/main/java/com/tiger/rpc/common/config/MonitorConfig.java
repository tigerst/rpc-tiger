package com.tiger.rpc.common.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @ClassName: MonitorConfig.java
 *
 * @Description: 监控配置
 *
 * @Author: Tiger
 *
 * @Date: 2019/4/28
 */
@Data
@Slf4j
public class MonitorConfig {
    /**
     * 同步初始延迟
     */
    private long syncInitialDelay = 5;

    /**
     * 同步延迟
     */
    private long syncDelay = 5;

    /**
     * 同步时间单位
     */
    private String syncTimeUnit = "SECONDS";

    /**
     * 协议服务监控初始延迟
     */
    private long serviceMonitorInitialDelay = 5;

    /**
     * 协议服务监控延迟
     */
    private long serviceMonitorDelay = 10;

    /**
     * 协议服务监控时间单位
     */
    private String serviceMonitorTimeUnit = "MINUTES";

    /**
     * zk上提供者监控初始延迟
     */
    private long zkProviderMonitorInitialDelay = 5;

    /**
     * zk上提供者监控延迟
     */
    private long zkProviderMonitorDelay = 8;

    /**
     * zk上提供者监控时间单位
     */
    private String zkProviderMonitorTimeUnit = "MINUTES";

    /**
     * 是否自动处理服务线程，默认不处理
     */
    private boolean isAutoProcessServiceThread = false;

    /**
     * 是否自动告警资源，默认为true
     */
    private boolean isAutoAlertForResource = true;

    /**
     * 是否自动告警服务，默认为true
     * 当服务被启用和停用时，通知
     */
    private boolean isAutoAlertForService = true;

    /**
     * 机器阈值：默认值为2
     */
    private int providersThreshold = 2;

    /**
     * 机器cpu使用率阈值：默认0.98d
     */
    private double cpuUsageThreshold = 0.98d;

    /**
     * 机器内存使用率阈值：默认0.95d
     */
    private double memUsageThreshold = 0.95d;

    /**
     * 机器磁盘使用率阈值：默认0.95d
     */
    private double diskUsageThreshold = 0.95d;

    public long getSyncInitialDelay() {
        return syncInitialDelay;
    }

    public MonitorConfig setSyncInitialDelay(long syncInitialDelay) {
        this.syncInitialDelay = syncInitialDelay;
        return this;
    }

    public long getSyncDelay() {
        return syncDelay;
    }

    public MonitorConfig setSyncDelay(long syncDelay) {
        this.syncDelay = syncDelay;
        return this;
    }

    public String getSyncTimeUnit() {
        return syncTimeUnit;
    }

    public MonitorConfig setSyncTimeUnit(String syncTimeUnit) {
        if(StringUtils.isBlank(syncTimeUnit)){
            return this;
        }
        this.syncTimeUnit = syncTimeUnit;
        return this;
    }

    public long getServiceMonitorInitialDelay() {
        return serviceMonitorInitialDelay;
    }

    public MonitorConfig setServiceMonitorInitialDelay(long serviceMonitorInitialDelay) {
        this.serviceMonitorInitialDelay = serviceMonitorInitialDelay;
        return this;
    }

    public long getServiceMonitorDelay() {
        return serviceMonitorDelay;
    }

    public MonitorConfig setServiceMonitorDelay(long serviceMonitorDelay) {
        this.serviceMonitorDelay = serviceMonitorDelay;
        return this;
    }

    public String getServiceMonitorTimeUnit() {
        return serviceMonitorTimeUnit;
    }

    public MonitorConfig setServiceMonitorTimeUnit(String serviceMonitorTimeUnit) {
        if(StringUtils.isBlank(serviceMonitorTimeUnit)){
            return this;
        }
        this.serviceMonitorTimeUnit = serviceMonitorTimeUnit;
        return this;
    }

    public long getZkProviderMonitorInitialDelay() {
        return zkProviderMonitorInitialDelay;
    }

    public MonitorConfig setZkProviderMonitorInitialDelay(long zkProviderMonitorInitialDelay) {
        this.zkProviderMonitorInitialDelay = zkProviderMonitorInitialDelay;
        return this;
    }

    public long getZkProviderMonitorDelay() {
        return zkProviderMonitorDelay;
    }

    public MonitorConfig setZkProviderMonitorDelay(long zkProviderMonitorDelay) {
        this.zkProviderMonitorDelay = zkProviderMonitorDelay;
        return this;
    }

    public String getZkProviderMonitorTimeUnit() {
        return zkProviderMonitorTimeUnit;
    }

    public MonitorConfig setZkProviderMonitorTimeUnit(String zkProviderMonitorTimeUnit) {
        if(StringUtils.isBlank(zkProviderMonitorTimeUnit)){
            return this;
        }
        this.zkProviderMonitorTimeUnit = zkProviderMonitorTimeUnit;
        return this;
    }

    public boolean isAutoProcessServiceThread() {
        return isAutoProcessServiceThread;
    }

    public MonitorConfig setAutoProcessServiceThread(boolean autoProcessServiceThread) {
        isAutoProcessServiceThread = autoProcessServiceThread;
        return this;
    }

    public int getProvidersThreshold() {
        return providersThreshold;
    }

    public MonitorConfig setProvidersThreshold(int providersThreshold) {
        this.providersThreshold = providersThreshold;
        return this;
    }

}

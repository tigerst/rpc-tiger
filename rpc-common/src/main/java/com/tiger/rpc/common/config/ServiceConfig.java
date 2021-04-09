package com.tiger.rpc.common.config;

import com.tiger.rpc.common.enums.ServiceStatus;
import com.tiger.rpc.common.utils.VMStat;
import com.google.common.base.Preconditions;

/**
 * @ClassName: ServiceConfig.java
 *
 * @Description: 注册服务配置
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/19
 */
public class ServiceConfig<T> extends BaseConfig<T> {

    /**
     * 服务端口
     */
    private int port;

    /**
     * 接口实现类引用（实际操作类）
     * 不序列化
     */
    private transient T ref;

    /**
     * 服务权重
     */
    private int weight = 1;

    /**
     * 其他可传参数，序列化后的json
     */
    private String otherMessage;

    /**
     * 虚拟机的最大可用的处理器数量
     */
    private int availableProcessors = VMStat.getProcessNum();

    /**
     * cpu使用率
     */
    private double cpuUsage = VMStat.getCpuUsage();

    /**
     * 内存使用率
     */
    private double memUsage = VMStat.getMemUsage();

    /**
     * 磁盘使用率
     */
    private double diskUsage =  VMStat.getDiskUsage();

    /**
     * 服务状态：ENABLED(1)、DISABLED(2)，默认可用
     */
    private ServiceStatus serviceStatus = ServiceStatus.ENABLED;

    public int getPort() {
        return port;
    }

    public ServiceConfig<T> setPort(int port) {
        //校验端口
        Preconditions.checkArgument(port >= 1024 && port <= 65535, "Invalid port: " + port);
        this.port = port;
        return this;
    }

    public T getRef() {
        return ref;
    }

    public ServiceConfig<T> setRef(T ref) {
        this.ref = ref;
        return this;
    }

    public int getWeight() {
        return weight;
    }

    public ServiceConfig<T> setWeight(int weight) {
        if(weight <= 0){
            this.weight = 1;
        } else {
            this.weight = weight;
        }
        return this;
    }

    public String getOtherMessage() {
        return otherMessage;
    }

    public ServiceConfig<T> setOtherMessage(String otherMessage) {
        this.otherMessage = otherMessage;
        return this;
    }


    public int getAvailableProcessors() {
        return availableProcessors;
    }

    public ServiceConfig<T> setAvailableProcessors(int availableProcessors) {
        this.availableProcessors = availableProcessors;
        return this;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public ServiceConfig<T> setCpuUsage(double cpuUsage) {
        this.cpuUsage = cpuUsage;
        return this;
    }

    public double getMemUsage() {
        return memUsage;
    }

    public ServiceConfig<T> setMemUsage(double memUsage) {
        this.memUsage = memUsage;
        return this;
    }

    public double getDiskUsage() {
        return diskUsage;
    }

    public ServiceConfig<T> setDiskUsage(double diskUsage) {
        this.diskUsage = diskUsage;
        return this;
    }

    public ServiceStatus getServiceStatus() {
        return serviceStatus;
    }

    public ServiceConfig<T> setServiceStatus(ServiceStatus serviceStatus) {
        this.serviceStatus = serviceStatus;
        return this;
    }
}

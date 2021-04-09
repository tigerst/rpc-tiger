package com.tiger.rpc.common.dto;

import com.tiger.rpc.common.utils.NetworkUtils;
import com.tiger.rpc.common.utils.ProcessUtils;
import com.tiger.rpc.common.utils.VMStat;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * @ClassName: MachineDto.java
 *
 * @Description: 机器信息包
 *
 * @Author: Tiger
 *
 * @Date: 2019/5/6
 */
@Data
public class MachinePacket implements Serializable {

    /**
     * 编号，默认uuid
     */
    private String id = UUID.randomUUID().toString();

    /**
     * 当前机器ip
     */
    private String ip = NetworkUtils.ip();

    /**
     * 当前机器主机名
     */
    private String hostName = NetworkUtils.host();

    /**
     * jvm程编号
     */
    private int pid = ProcessUtils.pid();

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
    private double diskUsage = VMStat.getDiskUsage();

    /**
     * 其他可传参数，序列化后的json
     */
    private String otherMessage;

    /**
     * 是否是master，默认false
     */
    private boolean isMaster = false;

    /**
     * jvm堆内存使用率（堆内存 / 已申请到的堆内存空间）
     */
    private double jvmHeapUsage = VMStat.getJvmHeap2CommittedUsage();

    /**
     * jvm堆内存使用率（堆内存 / 最大可用堆内存空间）
     */
    private double jvmHeapUsage2 = VMStat.getJvmHeap2MaxUsage();

}

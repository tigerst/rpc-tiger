package com.tiger.rpc.common.dto;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

/**
 * @ClassName: ServerConfig.java
 *
 * @Description: 机器信息配置包，用于传输外部
 *
 * @Author: Tiger
 *
 * @Date: 2019/5/6
 */
@Data
public class ServerPacket {

    /**
     * 机器信息
     */
    private MachinePacket machinePacket = new MachinePacket();

    /**
     * 机器建议配置服务列表
     */
    private List<ServicePacket> servicePackets = Lists.newArrayList();

}

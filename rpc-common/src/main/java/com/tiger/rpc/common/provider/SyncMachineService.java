package com.tiger.rpc.common.provider;

import com.tiger.rpc.common.dto.ServerPacket;

/**
 * @ClassName: SyncService.java
 *
 * @Description: sync同步机器上的服务信息到其他服务
 *
 * @Author: Tiger
 *
 * @Date: 2019/5/6
 */
public interface SyncMachineService {

    /**
     * 同步服务信息
     * @param serverPacket  服务器信息报（含服务列表）
     * @param operator 操作人
     * @throws Exception
     */
    public void syncServiceMsg(ServerPacket serverPacket, String operator) throws Exception;

}

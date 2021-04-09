package com.tiger.rpc.netty.packet;

import com.tiger.rpc.common.enums.ProtocolTypeEnum;
import lombok.Data;

/**
 * @ClassName: RpcPacket.java
 *
 * @Description: rpc基础包
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/30
 */
@Data
public class RpcPacket {

    /**
     * 请求编号
     */
    private String requestId;

    /**
     * rpc协议类型
     */
    private ProtocolTypeEnum protocolType;


}

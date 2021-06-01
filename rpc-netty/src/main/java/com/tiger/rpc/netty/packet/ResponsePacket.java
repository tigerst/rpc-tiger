package com.tiger.rpc.netty.packet;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName: ResponsePacket.java
 *
 * @Description: rpc响应数据包
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/30
 */
@Data
public class ResponsePacket extends RpcPacket implements Serializable {

    /**
     * 返回类型
     */
    private Class<?> returnType;

    /**
     * 响应结果
     */
    private Object result;

    /**
     * 异常
     */
    private Throwable throwable;

    /**
     * 是否返回标记
     */
    private boolean returnedFlag = false;

}

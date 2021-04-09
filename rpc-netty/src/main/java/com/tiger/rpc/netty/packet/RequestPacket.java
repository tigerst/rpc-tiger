package com.tiger.rpc.netty.packet;

import lombok.Data;

import java.io.Serializable;

/**
 * @ClassName: RequestPacket.java
 *
 * @Description: rpc请求交互数据包
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/30
 */
@Data
public class RequestPacket extends RpcPacket implements Serializable {

    /**
     * 接口名
     */
    private String className;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 方法参数类型
     */
    private Class[] paramType;

    /**
     * 方法参数
     */
    private Object[] args;

}

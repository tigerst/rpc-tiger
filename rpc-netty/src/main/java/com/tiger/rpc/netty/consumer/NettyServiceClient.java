package com.tiger.rpc.netty.consumer;

import com.tiger.rpc.common.enums.ProtocolTypeEnum;
import com.tiger.rpc.netty.consumer.handler.NettyClientHandler;
import com.tiger.rpc.netty.packet.RequestPacket;
import com.tiger.rpc.netty.packet.ResponsePacket;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.UUID;


/**
 * @ClassName: NettyServiceClient.java
 *
 * @Description: 远程连接客户端
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/30
 */
@Slf4j
public class NettyServiceClient {

    /**
     * tSocket连接对象
     */
    @Setter
    private NSocket nSocket;

    /**
     * 1.组装包：请求编号、类名、方法名、参数类型、真实参数值、返回类型
     * 2.缓存响应包
     * 3.执行远程rpc调
     * 4.将缓存中的响应包移除
     * 5.结果类型转换，弥补fastjson反序列化问题
     * 6.返回结果
     * @return
     */
    public Object syncSend(Method method, Object[] args) throws Throwable {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        try {
            if (this.nSocket == null) {
                throw new RuntimeException("No tSocket bind.");
            }

            RequestPacket requestRpc = new RequestPacket();
            requestRpc.setRequestId(requestId);
            requestRpc.setProtocolType(ProtocolTypeEnum.NETTY);
            Class<?> enClosedClazz = method.getDeclaringClass().getEnclosingClass();
            enClosedClazz = enClosedClazz == null? method.getDeclaringClass() : enClosedClazz;
            requestRpc.setClassName(enClosedClazz.getName());
            requestRpc.setMethodName(method.getName());
            requestRpc.setParamType(method.getParameterTypes());
            requestRpc.setArgs(args);

            ResponsePacket responseRpc = new ResponsePacket();
            responseRpc.setRequestId(requestId);
            responseRpc.setProtocolType(requestRpc.getProtocolType());
            responseRpc.setReturnType(method.getReturnType());
            //加入缓存
            NettyClientHandler.waitingRPC.put(requestId, responseRpc);
            //阻塞发送：线程等待，一直到有结果返回（ClientHandler调用了responseRpc对象，设置属性）。
            nSocket.writeAndFlush(requestRpc);
            //对象线程等待
            synchronized (responseRpc) {
                responseRpc.wait();
            }

            if (responseRpc.getThrowable() != null) {
                throw responseRpc.getThrowable();
            }

            Object object = responseRpc.getResult();
            return object;
        } catch (Exception e) {
            throw e;
        } finally {
            NettyClientHandler.waitingRPC.remove(requestId);
        }
    }

}

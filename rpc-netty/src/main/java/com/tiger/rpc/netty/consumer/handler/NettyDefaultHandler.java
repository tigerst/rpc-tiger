package com.tiger.rpc.netty.consumer.handler;

import com.alibaba.fastjson.JSON;
import com.tiger.rpc.common.consumer.handler.DefaultRpcHandler;
import com.tiger.rpc.common.helper.ReferenceHelper;
import com.tiger.rpc.netty.consumer.NSocket;
import com.tiger.rpc.netty.consumer.NettyServiceClient;
import com.tiger.rpc.netty.consumer.NettyServiceDiscovery;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;

/**
 * @ClassName: NettyDefaultHandler.java
 *
 * @Description: netty服务代理
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/30
 */
@Slf4j
public class NettyDefaultHandler extends DefaultRpcHandler<NSocket> implements InvocationHandler, Closeable {

    public NettyDefaultHandler(NettyServiceDiscovery discovery){
        super(discovery);
    }

    public NettyDefaultHandler setReferenceHelper(NettyServiceDiscovery discovery) {
        super.setHelper(new ReferenceHelper(discovery));
        return this;
    }

    public NettyDefaultHandler setReferenceHelper(ReferenceHelper helper) {
        super.setHelper(helper);
        return this;
    }

    /**
     * 异常处理
     * tSocket异常时，需要校验channel
     * 参数异常时，直接退出，不需要重试
     * @param exception
     * @param counter
     * @param key
     * @param tSocket
     * @throws Exception
     */
    @Override
    protected Throwable processException(Throwable exception, int counter, String key, NSocket tSocket) {
        if(exception instanceof SocketException){
            //tSocket异常，关闭channel，加速回收
            if(tSocket != null && tSocket.isOpen()){
                log.warn("Close the unreachable socket[{}] of key[{}]", tSocket, key);
                tSocket.close();
                tSocket = null;
            }
        }
        if(exception instanceof IllegalArgumentException){
            //参数异常，不重试，返回异常
            return exception;
        }
        if(exception instanceof InvocationTargetException){
            //反射异常，获取目标异常处理
            Throwable targetException = ((InvocationTargetException) exception).getTargetException();
            if(targetException instanceof IllegalArgumentException){
                //参数异常则返回目标异常
                return targetException;
            } else if (targetException instanceof SocketException){
                //tSocket异常引起的反射异常，校验tSocket，关闭tSocket，加速回收
                if(tSocket != null && tSocket.isOpen()){
                    log.warn("Close the unreachable socket[{}] of key[{}]", tSocket.getLocalSocketAddress(), key);
                    tSocket.close();
                    tSocket = null;
                }
            }
        }
        //重试比较
        if(counter >= super.getRetry()){
            //超过重试次数的，则抛出异常
            if(exception instanceof InvocationTargetException){
                //反射异常，抛出目标异常
                return ((InvocationTargetException) exception).getTargetException();
            }
            //返回异常
            return exception;
        }
        //返回null
        return null;
    }

    @Override
    protected Object getClient(NSocket tSocket, Method method) throws Exception {
        //设置channel，返回远程客户端
        NettyServiceClient nettyClient = new NettyServiceClient();
        nettyClient.setNSocket(tSocket);
        return nettyClient;
    }

    @Override
    protected Object callRemoteMethod(Object client, Method method, Object[] args) throws Throwable {
        //client执行同步发送
        return ((NettyServiceClient)client).syncSend(method, args);
    }

    @Override
    protected void processFinally(String key, Object client, NSocket tSocket) {
        log.debug("Release the current socket[{}] connected to provider[{}]",
                JSON.toJSONString(tSocket.getLocalSocketAddress()), key);
    }

    @Override
    public void close() throws IOException {
        /**
         * 置空参数，加速回收
         */
        super.close();
    }
}

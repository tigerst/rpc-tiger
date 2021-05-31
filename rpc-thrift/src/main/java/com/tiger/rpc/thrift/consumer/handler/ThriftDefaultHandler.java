package com.tiger.rpc.thrift.consumer.handler;

import com.alibaba.fastjson.JSON;
import com.tiger.rpc.common.consumer.handler.DefaultRpcHandler;
import com.tiger.rpc.common.helper.ReferenceHelper;
import com.tiger.rpc.thrift.consumer.ThriftServiceDiscovery;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @ClassName: DefaultThriftHandler.java
 *
 * @Description: thrift服务代理
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/23
 */
@Data
@Slf4j
public class ThriftDefaultHandler extends DefaultRpcHandler<TSocket> implements InvocationHandler, Closeable {

        /**
         * client工厂方法
         */
        private TServiceClientFactory<TServiceClient> clientFactory;

    public ThriftDefaultHandler(ThriftServiceDiscovery discovery){
            super(discovery);
        }

    public ThriftDefaultHandler setReferenceHelper(ThriftServiceDiscovery discovery) {
        super.setHelper(new ReferenceHelper(discovery));
        return this;
    }

    public ThriftDefaultHandler setReferenceHelper(ReferenceHelper helper) {
        super.setHelper(helper);
        return this;
    }

    /**
     * 异常处理
     * socket异常时，需要校验socket
     * 参数异常时，直接退出，不需要重试
     * @param exception
     * @param counter
     * @param key
     * @param tSocket
     * @throws Exception
     */
    @Override
    protected Throwable processException(Throwable exception, int counter, String key, TSocket tSocket) {
        if(exception instanceof TTransportException){
            //socket异常，关闭socket，加速回收
            if(tSocket != null && tSocket.isOpen()){
                log.warn("Close the unreachable socket[{}] of key[{}]", tSocket.getSocket().getLocalSocketAddress(), key);
                tSocket.close();
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
            } else if (targetException instanceof TTransportException){
                //socket异常引起的反射异常，校验socket，关闭socket，加速回收
                if(tSocket != null && tSocket.isOpen()){
                    log.warn("Close the unreachable socket[{}] of key[{}]", tSocket.getSocket().getLocalSocketAddress(), key);
                    tSocket.close();
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
    protected Object getClient(TSocket tSocket, Method method) throws Exception {
        //包装管道
        TTransport transport = new TFramedTransport(tSocket);
        //包装管道为字节协议
        TProtocol protocol = new TBinaryProtocol(transport);
        //多服务协议
        Class<?> enClosedClazz = method.getDeclaringClass().getEnclosingClass();
        enClosedClazz = enClosedClazz == null? method.getDeclaringClass() : enClosedClazz;
        TMultiplexedProtocol mpProtocol = new TMultiplexedProtocol(protocol, enClosedClazz.getName());
        TServiceClient client = this.clientFactory.getClient(mpProtocol);
        return client;
    }

    @Override
    protected void processFinally(String key, Object client, TSocket tsocket) {
        log.debug("Release the current socket[{}] connected to provider[{}]",
                JSON.toJSONString(tsocket.getSocket().getLocalSocketAddress()), key);
    }

    @Override
    public void close() throws IOException {
        /**
         * 置空参数，加速回收
         */
        super.close();
        this.clientFactory = null;
    }
}

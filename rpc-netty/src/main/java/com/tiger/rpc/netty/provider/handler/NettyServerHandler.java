package com.tiger.rpc.netty.provider.handler;

import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.netty.packet.RequestPacket;
import com.tiger.rpc.netty.packet.ResponsePacket;
import io.netty.channel.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


/**
 * @ClassName: NettyServerHandler.java
 *
 * @Description: Netty服务端处理
 *              注解成Sharable共享NettyServerHandler，处理多客户端（或者客户端重启）io.netty.channel.StacklessClosedChannelException异常问题
 *
 * @Author: Tiger
 *
 * @Date: 2021/4/1
 */
@Slf4j
@ChannelHandler.Sharable
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    /**
     * 接口名和接口引用缓存，所有channel公用
     */
    @Getter
    private final Map<String, Object> processor = new HashMap<>();

    public NettyServerHandler(Map<String, ServiceConfig> beans) {
        beans.values().stream().forEach(o -> {
            //解析注册服务，缓存接口名引用
            processor.put(o.getInterfaceClass().getName(), o.getRef());
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            //空对象，直接抛弃
            return;
        }
        RequestPacket requestRpc = (RequestPacket)msg;
        if (StringUtils.isBlank(requestRpc.getRequestId()) || requestRpc.getProtocolType() == null) {
            //无编号 or 无协议类型，直接抛弃
            return;
        }
        //创建响应体，并同步请求编号 & 协议类型
        ResponsePacket responseRpc = new ResponsePacket();
        responseRpc.setRequestId(requestRpc.getRequestId());
        responseRpc.setProtocolType(requestRpc.getProtocolType());
        try {
            if(StringUtils.isBlank(requestRpc.getClassName())){
                new RuntimeException("Param[className] can not be null");
            }
            if(StringUtils.isBlank(requestRpc.getMethodName())){
                new RuntimeException("Param[methodName] can not be null");
            }
            //通过服务注册表，获取方法，用该实例反射方式执行方法获取结果
            Object bean = processor.get(requestRpc.getClassName());
            if (bean == null) {
                //未发现实例引用时，尝试查找类加载器中的类，并创建实例
                log.warn("No instance of Class[{}] found for the request[{}], and will retry to create a new instance", requestRpc.getClassName(), requestRpc.getRequestId());
                try {
                    bean = Class.forName(requestRpc.getClassName()).newInstance();
                } catch (Exception e) {
                    new RuntimeException(String.format("No service for class[%s] found.", requestRpc.getClassName()));
                }
            }
            Method method = bean.getClass().getMethod(requestRpc.getMethodName(), requestRpc.getParamType());
            if (method == null) {
                //未查到方法的异常抛出
                new RuntimeException(String.format("Method[%s] no found", requestRpc.getMethodName()));
            }

            Object result = method.invoke(bean, requestRpc.getArgs());
            //执行成供后把结果放入响应包中
            responseRpc.setResult(result);
        } catch (Exception e){
            //设置异常
            log.error(String.format("Process request[%s] failed", responseRpc.getRequestId()), e);
            responseRpc.setThrowable(e);
        }

        //回写响应
        ctx.writeAndFlush(responseRpc).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                log.info(responseRpc.getRequestId() + " processed.");
            }
        });
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        super.channelReadComplete(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
    }

}

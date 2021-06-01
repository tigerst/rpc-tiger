package com.tiger.rpc.netty.consumer.handler;

import com.tiger.rpc.netty.packet.ResponsePacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;


/**
 * @ClassName: NettyClientHandler.java
 *
 * @Description: 客户端处理器
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/30
 */
@Slf4j
public class NettyClientHandler extends SimpleChannelInboundHandler<ResponsePacket> {

    /**
     * 响应包缓存
     */
    public static ConcurrentHashMap<String, ResponsePacket> waitingRPC = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ResponsePacket responseRpc) throws Exception {
        /**
         * 回写调用结果 & 异常信息
         */
        ResponsePacket cachedResponseRpc = waitingRPC.get(responseRpc.getRequestId());
        if (cachedResponseRpc != null) {
            synchronized (cachedResponseRpc) {
                //加对象锁，设置属性，并唤醒wait对象线程，继续执行代码
                cachedResponseRpc.setReturnedFlag(true);
                cachedResponseRpc.setThrowable(responseRpc.getThrowable());
                cachedResponseRpc.setResult(responseRpc.getResult());
                cachedResponseRpc.notify();
            }
        } else {
            log.warn("No cached response[requestId={}, protocolType={}] found.", responseRpc.getRequestId(), responseRpc.getProtocolType());
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelRegistered();
    }
}

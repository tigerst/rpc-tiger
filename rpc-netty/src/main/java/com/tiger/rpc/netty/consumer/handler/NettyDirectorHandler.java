package com.tiger.rpc.netty.consumer.handler;

import com.tiger.rpc.netty.consumer.NettyServiceDiscovery;
import lombok.Data;

import java.io.IOException;

/**
 * @ClassName: NettyDirectorHandler.java
 *
 * @Description: 指定机器处理器
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/30
 */
public class NettyDirectorHandler extends NettyDefaultHandler {

    public NettyDirectorHandler(NettyServiceDiscovery discovery){
        super(discovery);
    }

    @Override
    public void close() throws IOException {
        /**
         * 置空参数，加速回收
         */
        super.close();
    }

}

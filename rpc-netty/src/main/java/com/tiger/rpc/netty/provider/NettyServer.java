package com.tiger.rpc.netty.provider;

import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.common.enums.ProtocolTypeEnum;
import com.tiger.rpc.netty.packet.ResponsePacket;
import com.tiger.rpc.netty.code.RpcDecoder;
import com.tiger.rpc.netty.code.RpcEncoder;
import com.tiger.rpc.netty.packet.RequestPacket;
import com.tiger.rpc.netty.provider.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @ClassName: NettyServer.java
 *
 * @Description: netty服务管理
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/30
 */
@Slf4j
public final class NettyServer {

    /**
     * netty服务名
     */
    @Getter
    private String name;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private ChannelFuture server;

    private NettyServerHandler serverHandler;

    NettyServer(String name, Map<String, ServiceConfig> beans, int port, int selectorThreads, int workerThreads) {
        //设置线程名称
        this.name = name;

        //设置selector线程数
        bossGroup = new NioEventLoopGroup(selectorThreads);
        //设置worker线程数
        workerGroup = new NioEventLoopGroup(workerThreads);
        //服务端处理器，处理服务具体方法
        serverHandler = new NettyServerHandler(beans);
        ServerBootstrap bootstrap = new ServerBootstrap().group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) throws Exception {
                        //解码/编码与客户端顺序相反，最后设置服务端处理器
                        channel.pipeline()
                                //解码请求数据包
                                .addLast(new RpcDecoder<RequestPacket>(RequestPacket.class, ProtocolTypeEnum.NETTY))
                                //编码响应数据包
                                .addLast(new RpcEncoder<ResponsePacket>(ResponsePacket.class))
                                //服务处理器
                                .addLast(serverHandler);
                    }
                })
                //绑定so_backlog(最大连接数1024)、keep-alive(探测客户端的连接是否还存活着)、tcp_nodelay(tcp非延迟发送)
                .option(ChannelOption.SO_BACKLOG, 1024).childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);
        //绑定端口
        server = bootstrap.bind(port);
    }

    /**
     * 运行时加服务
     * @param services
     */
    public void addServices(List<ServiceConfig> services){
        if(this.serverHandler != null){
            Class<?> enClosedClazz;
            for (ServiceConfig config : services) {
                enClosedClazz = config.getInterfaceClass().getEnclosingClass();
                enClosedClazz = enClosedClazz == null? config.getInterfaceClass() : enClosedClazz;
                this.serverHandler.getProcessor().put(enClosedClazz.getName(), config.getRef());
            }
        }
    }

    /**
     * 服务启动
     */
    public void start() {
        log.debug("Start to start server thread");
        if (server != null) {
            while (server != null && !server.channel().isActive()) {
                try {
                    //启动rpc服务
                    server.sync();
                } catch (Exception e) {
                    log.error("Process error", e);
                }
            }
        }
        log.debug("Server thread started");
    }

    /**
     * 是否在服务中
     * @return
     */
    public boolean isServing(){
        boolean servingFlag = server != null && server.channel() != null ? server.channel().isActive() : false;
        if(servingFlag){
            log.debug("Netty server is serving");
        } else {
            log.debug("Netty server is not serving");
        }
        return servingFlag;
    }

    /**
     * 停服时关闭资源
     */
    public void stopServer() {
        log.debug("Start to stop server thread");
        if (server != null) {
            server.channel().closeFuture();
            server = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        log.debug("Server thread stopped successfully");
    }
}

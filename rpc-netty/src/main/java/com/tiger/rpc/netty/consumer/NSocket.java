package com.tiger.rpc.netty.consumer;

import com.tiger.rpc.common.enums.ProtocolTypeEnum;
import com.tiger.rpc.netty.code.RpcDecoder;
import com.tiger.rpc.netty.code.RpcEncoder;
import com.tiger.rpc.netty.consumer.handler.NettyClientHandler;
import com.tiger.rpc.netty.packet.RequestPacket;
import com.tiger.rpc.netty.packet.ResponsePacket;
import com.tiger.rpc.netty.packet.RpcPacket;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.ToString;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.concurrent.ExecutionException;

/**
 * @ClassName: NSocket.java
 *
 * @Description: Netty tSocket
 *
 * @Author: Tiger
 *
 * @Date: 2021/4/4
 */
@ToString
public class NSocket implements Closeable {

    /**
     * Socket远程地址
     */
    private final String host;

    /**
     * Socket远程端口
     */
    private final int port;

    /**
     * Socket耗时设置
     */
    private Integer timeout;

    /**
     * netty channel，不对外暴露
     */
    private Channel channel;

    /**
     * 事件线程池
     */
    private EventLoopGroup group;

    public NSocket(String host, int port) {
        this.host = host;
        this.port = port;
        this.timeout = 0;
    }

    public NSocket(String host, int port , Integer timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    /**
     * 打开socket
     * @return
     * @throws InterruptedException
     */
    public Channel open() throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        //创建事件线程池，固定2个线程处理该channel，防止线程膨胀
        group = new NioEventLoopGroup(2);
        bootstrap.group(group).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel channel) throws Exception {
                        //解码/编码与服务端顺序相反，最后设置客户端处理器
                        channel.pipeline()
                                //编码请求数据包
                                .addLast(new RpcEncoder<RequestPacket>(RequestPacket.class))
                                //解码响应数据包
                                .addLast(new RpcDecoder<ResponsePacket>(ResponsePacket.class, ProtocolTypeEnum.NETTY))
                                //客户端处理器
                                .addLast(new NettyClientHandler());
                    }
                }).option(ChannelOption.TCP_NODELAY, true);
        if(timeout != null && timeout > 0) {
            //超时连接(如果不设置超时，连接会一直占用本地线程，端口，连接客户端一多，会导致本地端口用尽及CPU压力)
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout).option(ChannelOption.SO_TIMEOUT, timeout);
            this.channel = bootstrap.connect(host, port).awaitUninterruptibly().channel();
        } else {
            //同步连接
            this.channel = bootstrap.connect(host, port).sync().channel();
        }
        return channel;
    }

    /**
     * check是否打开连接
     * @return
     */
    public boolean isOpen() {
        if (channel == null || group == null || group.isShutdown()) {
            //channel为null or group为null or group关闭，则返回null
            return false;
        }
        return channel.isOpen();
    }

    /**
     * 把channel和group都关闭，设置为null加速回收对象
     */
    @Override
    public void close(){
        if (channel != null && channel.isActive()) {
//            channel.closeFuture();
            channel.close();
            channel = null;
        }
        if (group != null) {
            group.shutdownGracefully();
            group = null;
        }
    }

    /**
     * 获取socket本地连接地址
     * @return
     */
    public SocketAddress getLocalSocketAddress() {
        if (channel != null){
            return channel.localAddress();
        }
        return null;
    }

    /**
     * 同步写入，线程阻塞到响应结果
     * @param object
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public <T extends RpcPacket> void writeAndFlush(T object) throws InterruptedException, ExecutionException {
        if (channel == null || !channel.isOpen()) {
            this.open();
        }
        channel.writeAndFlush(object).sync();
    }

}

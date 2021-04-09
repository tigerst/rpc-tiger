package com.tiger.rpc.netty.code;

import com.alibaba.fastjson.JSON;
import com.tiger.rpc.netty.packet.RpcPacket;
import com.tiger.rpc.netty.utils.ProtoStuffUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @ClassName: RpcEncoder.java
 *
 * @Description: rpc包编码器
 *
 * @Author: Tiger
 *
 * @Date: 2021/4/1
 *
 * @param <T>   范型
 */
public class RpcEncoder<T extends RpcPacket> extends MessageToByteEncoder {

    /**
     * rpc包类型
     */
    private Class<T> rpcPacket;

    public RpcEncoder(Class<T> rpcPacket){
        this.rpcPacket = rpcPacket;
    }

    /**
     * 数据包编码：protocol dataLength data｜protocol dataLength data｜protocol dataLength data
     *          1.使用json序列化数据包
     *          2.写入协议信息
     *          3.写入包长度
     *          4.写入包数据
     * @param ctx
     * @param rpcDataBody   rpc数据传输包
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Object rpcDataBody, ByteBuf out) throws Exception {
        //使用json序列化数据包
//        byte[] data = JSON.toJSONBytes(rpcDataBody);
        //使用protoStuff序列化数据包
        byte[] data = ProtoStuffUtil.serialize(rpcDataBody);
        //1.写入协议信息
        T t = rpcPacket.cast(rpcDataBody);
        out.writeBytes(t.getProtocolType().getValue().getBytes());
        //2.写入包长度
        out.writeInt(data.length);
        //3.写入包数据
        out.writeBytes(data);
    }
}

package com.tiger.rpc.netty.code;

import com.alibaba.fastjson.JSON;
import com.tiger.rpc.common.enums.ProtocolTypeEnum;
import com.tiger.rpc.netty.packet.RpcPacket;
import com.tiger.rpc.netty.utils.ProtoStuffUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @ClassName: RpcDecoder.java
 *
 * @Description: rpc包解码器：拆包和粘包
 *
 * @Author: Tiger
 *
 * @Date: 2021/4/1
 *
 * @param <T>   范型
 */
public class RpcDecoder<T extends RpcPacket> extends ByteToMessageDecoder {

    /**
     * 是否有信息头部标记
     */
    private boolean hasMsgHead = false;

    /**
     * 信息头数组大小(netty)
     */
    private final byte[] msgHeadBuffer;

    /**
     * rpc包类型
     */
    private Class<T> rpcPacket;

    public RpcDecoder(Class<T> rpcPacket, ProtocolTypeEnum protocolType) {
        this.rpcPacket = rpcPacket;
        if (protocolType == null) {
            //如果为null，则默认使用netty协议
            protocolType = ProtocolTypeEnum.NETTY;
        }
        msgHeadBuffer = new byte[protocolType.getValue().getBytes().length];
    }

    /**
     * 数据包解码：protocol dataLength data｜protocol dataLength data｜protocol dataLength data
     *          1.协议检测
     *          2.含有协议头数据包，读取交互数据包的大小
     *          3.比较交互数据包的大小与tcp数据包（头信息大小）的大小
     *          4.对于多数据包，进行拆包处理
     *          5.对于残缺数据包，进行粘包处理
     *          6.使用json反序列化数据包
     *          7.将反序列化的包输出
     * @param ctx
     * @param in
     * @param out
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //hasMsgHead避免多次判断头信息
        if (!hasMsgHead) {
            while (true) {
                //这里保证至少读到一个头信息，也可以读到一个头和数据长度再做处理
                if (in.readableBytes() < msgHeadBuffer.length) {
                    return;
                }
                in.markReaderIndex();
                in.readBytes(msgHeadBuffer);
                String s = new String(msgHeadBuffer);
                if (s.equals(ProtocolTypeEnum.NETTY.getValue())) {
                    hasMsgHead = true;
                    break;
                } else {
                    in.resetReaderIndex();
                    in.readByte();
                }
            }
        }

        /**
         * 若当前可以获取到的字节数小于实际长度，则直接返回，直到当前可以获取到的字节数等于实际长度
         */
        in.markReaderIndex();
        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }
        hasMsgHead = false;
        //读取完整的消息体字节数组
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        //使用json反序列化数据包
//        T  t = JSON.parseObject(data, rpcPacket);
        //使用protoStuff反序列化数据包
        T t = ProtoStuffUtil.deserialize(data, rpcPacket);
        //将反序列化的包输出
        out.add(t);
    }
}

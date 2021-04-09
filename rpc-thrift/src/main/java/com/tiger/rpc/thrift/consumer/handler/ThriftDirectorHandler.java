package com.tiger.rpc.thrift.consumer.handler;

import com.tiger.rpc.thrift.consumer.ThriftServiceDiscovery;
import lombok.Data;

import java.io.IOException;

/**
 * @ClassName: FixedThriftHandler.java
 *
 * @Description: 指定机器处理器
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/23
 */
@Data
public class ThriftDirectorHandler extends ThriftDefaultHandler {

    public ThriftDirectorHandler(ThriftServiceDiscovery discovery){
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

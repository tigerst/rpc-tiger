package com.tiger.rpc.thrift.provider;

import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.thrift.utils.ThriftUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TTransportException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @ClassName: ThriftServerThread.java
 *
 * @Description: thift服务线程
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/31
 */
@Slf4j
public final class ThriftServerThread extends Thread {
    /**
     * thrift 服务
     */
    @Getter
    private TServer server;

    private TMultiplexedProcessor processor;

    ThriftServerThread(String name, Map<String, ServiceConfig> beans, int port, int selectorThreads, int workerThreads) throws TTransportException {
        //设置线程名称
        this.setName(name);
        //守护线程
        this.setDaemon(true);
        //nio方式
        TNonblockingServerSocket serverTransport = new TNonblockingServerSocket(port);
        TThreadedSelectorServer.Args tArgs = new TThreadedSelectorServer.Args(serverTransport);
        //设置selector线程数
        tArgs.selectorThreads(selectorThreads);
        //设置worker线程数
        tArgs.workerThreads(workerThreads);
        //多服务Processor
        if(this.processor == null){
            this.processor = new TMultiplexedProcessor();
        }
        Set<Map.Entry<String, ServiceConfig>> beanEntrySet = beans.entrySet();
        Object bean;
        Class<?> ifaceClass;
        Class<?> enClosedClazz;
        for (Map.Entry<String, ServiceConfig> entry : beanEntrySet) {
            //引用的实例
            bean = entry.getValue().getRef();
            //获取接口对象
            ifaceClass = entry.getValue().getInterfaceClass();
            enClosedClazz = ifaceClass.getDeclaringClass().getEnclosingClass();
            enClosedClazz = enClosedClazz == null? ifaceClass.getDeclaringClass() : enClosedClazz;
            //注册服务
            this.processor.registerProcessor(enClosedClazz.getName(), ThriftUtils.getServiceProcessor(bean, ifaceClass));
        }

        TProcessorFactory processorFactory = new TProcessorFactory(this.processor);
        tArgs.processorFactory(processorFactory);
        tArgs.transportFactory(new TFramedTransport.Factory());
        tArgs.protocolFactory(new TBinaryProtocol.Factory(true, true));
        server = new TThreadedSelectorServer(tArgs);
    }

    /**
     * 运行时加服务
     * @param services
     */
    public void addServices(List<ServiceConfig> services){
        if(this.processor == null){
            this.processor = new TMultiplexedProcessor();
        }
        for (ServiceConfig config : services) {
            Class<?> enClosedClazz = config.getInterfaceClass().getEnclosingClass();
            enClosedClazz = enClosedClazz == null? config.getInterfaceClass() : enClosedClazz;
            this.processor.registerProcessor(enClosedClazz.getName(), ThriftUtils.getServiceProcessor(config.getRef(), config.getInterfaceClass()));
        }
    }

    @Override
    public void run() {
        log.debug("Start to start server thread");
        if (server != null) {
            server.serve();
        }
        log.debug("Server thread started");
    }

    /**
     * 是否在服务中
     * @return
     */
    public boolean isServing(){
        boolean servingFlag = server != null ? server.isServing() : false;
        if(servingFlag){
            log.debug("Thrift server is serving");
        } else {
            log.debug("Thrift server is not serving");
        }
        return servingFlag;
    }

    public void stopServer() {
        log.debug("Start to stop server thread");
        if (server != null) {
            server.stop();
        }
        log.debug("Server thread stopped successfully");
    }
}

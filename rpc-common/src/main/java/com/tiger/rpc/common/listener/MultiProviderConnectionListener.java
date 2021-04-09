package com.tiger.rpc.common.listener;

import com.tiger.rpc.common.register.ServiceRegister;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;

/**
 * @ClassName: MultiProviderConnectionListener.java
 *
 * @Description: 多服务的provider连接监听器
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/30
 */
@Slf4j
public class MultiProviderConnectionListener implements ConnectionStateListener {

    private final ServiceRegister register;

    public MultiProviderConnectionListener(ServiceRegister register) {
        this.register = register;
    }

    @Override
    public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
        switch (connectionState){
            case LOST:
                //断开连接后，只监控
                try {
                    log.info("Zookeeper lost, start to unRegister services...");
//                        unRegister();
                    log.info("Zookeeper lost, unRegister services successfully");
                } catch (Exception e) {
                    log.error("Zookeeper lost, unRegister services error", e);
                }
                break;
            case RECONNECTED:
                //重新连接，注册服务，并设置注册状态
                try {
                    log.info("Zookeeper reconnected, start to register services...");
                    register.reRegister();
                    log.info("Zookeeper reconnected, register services successfully");
                } catch (Exception e) {
                    log.error("Zookeeper reconnected, register services error", e);
                }
                break;
        }
    }
}

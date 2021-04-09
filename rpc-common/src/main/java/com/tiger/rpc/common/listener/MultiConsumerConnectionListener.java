package com.tiger.rpc.common.listener;

import com.tiger.rpc.common.register.ReferenceRegister;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;

/**
 * @ClassName: ConsumerListConnectionListener.java
 *
 * @Description: 多服务的consumer连接监听器
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/30
 */
@Slf4j
public class MultiConsumerConnectionListener implements ConnectionStateListener {

    private final ReferenceRegister register;

    public MultiConsumerConnectionListener(ReferenceRegister register) {
        this.register = register;
    }

    @Override
    public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
        switch (connectionState){
            case LOST:
                //断开连接后，只监控
                try {
                    log.info("Zookeeper lost, start to unDiscovery services...");
//                        unDiscovery();
                    log.info("Zookeeper lost, unDiscovery services successfully");
                } catch (Exception e) {
                    log.error("Zookeeper lost, unDiscovery services error", e);
                }

                break;
            case RECONNECTED:
                //重新连接，注册consumer，发现providers
                try {
                    log.info("Zookeeper reconnected, start to discovery services...");
                    register.reDiscovery();
                    log.info("Zookeeper reconnected, discovery services successfully");
                } catch (Exception e) {
                    log.error("Zookeeper reconnected, discovery services error", e);
                }

                break;

            default:
                break;
        }
    }
}

package com.tiger.rpc.common.listener;

import com.tiger.rpc.common.register.ReferenceRegister;
import com.tiger.rpc.common.utils.Constants;
import com.tiger.rpc.common.utils.ProviderParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import java.util.Collections;

/**
 * @ClassName: MultiServiceProvidersListener.java
 *
 * @Description: 多服务的providers子节点监听
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/30
 */
@Slf4j
public class MultiServiceProvidersListener implements PathChildrenCacheListener {
    
    private final ReferenceRegister register;

    public MultiServiceProvidersListener(ReferenceRegister register) {
        this.register = register;
    }

    @Override
    public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
        //provider路径：servicePath/providers/127.0.0.0:9090:2
        String childPath = null;
        //路径分割后的数组
        String[] paths = null;
        //机器地址:端口:权重
        String provider = null;
        //服务：接口_版本号
        String referenceService = null;
        //根据不通的事件类型做不同处理
        switch (pathChildrenCacheEvent.getType()){
            case CHILD_ADDED:
                log.debug("Find a new provider to be on line");
                childPath = pathChildrenCacheEvent.getData().getPath();
                //应用路径/服务/providers/host
                paths = childPath.split(Constants.PATH_SEPARATOR);
                //取服务机器
                provider = paths[paths.length - 1];
                //取服务名
                referenceService = paths[paths.length - 3];
                //将上线的provider(含有权重时，需要拆分成多个)加入暴露服务中
                register.getServiceProvidersMap().get(referenceService).addAll(ProviderParser.parseSingleProvider(provider));
                //打乱服务的provider的顺序
                Collections.shuffle(register.getServiceProvidersMap().get(referenceService));
                log.debug("Provider[{}] online successfully", provider);
                break;
            case CHILD_REMOVED:
                log.debug("Find a provider to to be off line");
                childPath = pathChildrenCacheEvent.getData().getPath();
                paths = childPath.split(Constants.PATH_SEPARATOR);
                provider = paths[paths.length - 1];
                referenceService = paths[paths.length - 3];
                //将下线的provider(含有权重时，需要拆分成多个)移除
                register.getServiceProvidersMap().get(referenceService).removeAll(ProviderParser.parseSingleProvider(provider));
                log.debug("Provider[{}] offline successfully", provider);
                break;

            default:
                break;
        }
    }
}

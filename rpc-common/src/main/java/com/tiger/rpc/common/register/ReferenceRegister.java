package com.tiger.rpc.common.register;

import com.alibaba.fastjson.JSON;
import com.tiger.rpc.common.config.ReferenceConfig;
import com.tiger.rpc.common.consumer.policy.ProviderStrategy;
import com.tiger.rpc.common.listener.MultiConsumerConnectionListener;
import com.tiger.rpc.common.listener.MultiServiceProvidersListener;
import com.tiger.rpc.common.utils.Constants;
import com.tiger.rpc.common.utils.ProviderParser;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.zookeeper.CreateMode;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @ClassName: ReferenceRegister.java
 *
 * @Description: consumer注册，不支持动态加入引入服务
 *               引用时，创建代理proxy
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/19
 */
@Slf4j
public abstract class ReferenceRegister implements Closeable {

    /**
     * 应用注册器
     */
    private ApplicationRegister appRegister;

    /**
     * 服务名_版本号 <---> provider list映射
     */
    private final Map<String, List<String>> serviceProvidersMap = new ConcurrentHashMap<String, List<String>>();

    /**
     * 接口(xxx.Iface)<--->ReferenceConfig
     */
    private Map<String, ReferenceConfig> referenceBeanMap = new ConcurrentHashMap<String, ReferenceConfig>();

    /**
     * 选择策略
     */
    private ProviderStrategy<String> providerStrategy;

    /**
     * 是否注册标记
     */
    protected transient volatile boolean isRegistered;

    /**
     * zk监听
     */
    private transient ConnectionStateListener zkConnectionListener = new MultiConsumerConnectionListener(this);

    /**
     * 子节点监听器
     */
    private transient MultiServiceProvidersListener childListener;

    /**
     * 服务名_版本号<---->子节点监听Cache
     */
    private Map<String, PathChildrenCache> serviceCacheMap = new ConcurrentHashMap<>();

    /**
     * 无参构造器
     */
    public ReferenceRegister(){
    }

    /**
     * 含参构造器
     * @param appRegister   应用注册
     * @param referenceList 引用服务列表
     * @param selectPolicy  选择策略
     */
    public ReferenceRegister(ApplicationRegister appRegister, List<ReferenceConfig> referenceList, ProviderStrategy<String> selectPolicy){
        Preconditions.checkArgument(appRegister != null, "appRegister can not be null");
        Preconditions.checkArgument(appRegister.isRegistered(), "Application must be registered before references");
        this.appRegister = appRegister;
        //过滤服务
        doFilterServiceList(referenceList);
        //填充代理类
        doFillProxy(Lists.newArrayList(this.referenceBeanMap.values()));
        //将引入服务注册器回写
        this.appRegister.addReferenceRegister(this);
        this.providerStrategy = selectPolicy;
    }

    /**
     * 过滤出协议的服务
     * @param referenceList
     */
    protected abstract List<ReferenceConfig> doFilterServiceList(List<ReferenceConfig> referenceList);

    /**
     * 重连后重新注册，跳过监听（此时已存在监听）
     * @throws Exception
     */
    public void reDiscovery() throws Exception {
        if (validDependencies()) {
            return;
        }
        if(isRegistered){
            //1.已经注册，重新注册consumer
            registerConsumer(this.referenceBeanMap.values());
            return;
        }

        //2.发现服务
        discoveryProviders(this.referenceBeanMap.values());

        //3.具体注册
        doDiscovery();
        //4.设置标记
        isRegistered = true;
    }

    /**
     * 注册服务，并启动监听zk连接
     */
    public void discovery() throws Exception {
        if (validDependencies()) {
            return;
        }

        //1.注册consumer
        registerConsumer(this.referenceBeanMap.values());

        //2.发现服务
        discoveryProviders(this.referenceBeanMap.values());

        //3.具体发现服务
        doDiscovery();

        //4.zk连接监听
        appRegister.getZkClient().getConnectionStateListenable().addListener(zkConnectionListener);

        //5.设置注册成功标记
        this.isRegistered = true;

    }

    private boolean validDependencies() throws Exception {
        if(!appRegister.isRegistered()){
            //先注册应用
            appRegister.register();
        }
        if(CollectionUtils.isEmpty(this.referenceBeanMap.values())){
            log.info("No service to register");
            return true;
        }
        return false;
    }

    /**
     * 注册具体的协议服务
     * @throws Exception
     */
    protected abstract void doDiscovery() throws Exception;

    /**
     * 注册机器，到consumer目录下
     * @param configs   服务集
     * @throws Exception
     */
    private void registerConsumer(Collection<ReferenceConfig> configs) throws Exception {
        //注册信息
        for (ReferenceConfig config : configs) {
            try {
                //机器地址：appPath/protocol(thrift/netty)/com.tiger.chaos.UserService_1.0.0(服务名)/consumers/127.0.0.0
                String consumerPath = assembleConsumerPath(config);
                //创建ephemeral消费节点, 记录service discovery信息
                if(appRegister.getZkClient().checkExists().forPath(consumerPath) == null){
                    //如果不存在，则创建；否则跳过
                    appRegister.getZkClient().create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).
                            forPath(consumerPath, JSON.toJSONString(config).getBytes(Constants.DEFAULT_CHARSET));
                }
            } catch (Exception e) {
                log.error("register service[{}] consumers[{}] error", config.getInterfaceName(), config.getHost(), e);
                throw e;
            }
        }
    }

    /**
     * 发现服务
     * @param configs   服务集
     * @throws Exception
     */
    private void discoveryProviders(Collection<ReferenceConfig> configs) throws Exception {
        //发现服务提供者providers
        //子节点（provider节点）监听器不存在，则创建，否则重复使用
        if(this.childListener == null){
            this.childListener = new MultiServiceProvidersListener(this);
        }
        StringBuffer sb = new StringBuffer();
        List<String> activeServices = Lists.newArrayList();
        for (ReferenceConfig config : configs) {
            //清空
            sb.setLength(0);
            //加入应用路径
            sb = new StringBuffer(appRegister.getAppPath());
            //获取服务名(service_1.0.0)
            String serviceName = getServiceNameByConf(config);
            //应用路径/协议/服务/providers
            String providersPath = assembleProvidersPath(config);
            if(appRegister.getZkClient().checkExists().forPath(providersPath) == null){
                //不存在，创建
                appRegister.getZkClient().create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(providersPath);
            }
            //服务的所有provider
            List<String> children = appRegister.getZkClient().getChildren().forPath(providersPath);
            //获取providers，加上权重; 将服务_版本号和provider list组成映射存储
            serviceProvidersMap.put(serviceName, ProviderParser.parseProviders(children));
            if(serviceCacheMap.containsKey(serviceName)){
                //已经存在，则跳过子节点监听器注册
                continue;
            }
            //监听服务的协议（protocol）机器变化：机器在应用路径/协议/服务/providers目录下
            PathChildrenCache cache = new PathChildrenCache(appRegister.getZkClient(), providersPath, true);
            //加入子节点监听
            cache.getListenable().addListener(this.childListener);
            //后台初始化
            cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
            //加入map缓存起来
            serviceCacheMap.put(serviceName, cache);
            //临时存储服务名
            activeServices.add(serviceName);
        }

        // TODO: 2019/8/5  处理废弃服务：暂不需要
//        processAbandonedServices(activeServices);

        //打印服务发现和注册消费者日志
        List<String> nameServiceList = CollectionUtils.isEmpty(configs)? new ArrayList<>() :
                configs.stream().map(o -> {
                    Class<?> enClosedClazz = o.getInterfaceClass().getEnclosingClass();
                    enClosedClazz = enClosedClazz == null ? o.getInterfaceClass() : enClosedClazz;
                    return enClosedClazz.getName() + Constants.SERVICE_VERSION_SEPARATOR + o.getVersion();
                }).collect(Collectors.toList());
        log.debug("ServiceList[{}] discovered from zookeeper, register consumer to zookeeper", StringUtils.join(nameServiceList, ","));
    }

    /**
     * 处理废弃服务:
     *      1.遍历所有服务，过滤出废弃服务
     *      2.将废弃服务的provider清空
     *      3.关闭废弃服务的子节点监听器
     * @param activeServices
     */
    private void processAbandonedServices(List<String> activeServices) {
        //复制一份所有服务
        List<String> allServices = Lists.newArrayList(serviceProvidersMap.keySet());
        //遍历服务
        allServices.stream().forEach(o -> {
            if(activeServices.contains(o)){
                //如果是激活服务，跳过
                return;
            }
            //废弃服务
            serviceProvidersMap.remove(o);
            log.debug("Remove providers of abandoned service[{}] successfully,", o);
            /**
             * 关闭废弃服务的子节点监听
             */
            PathChildrenCache cache = serviceCacheMap.remove(o);
            try {
                if(cache != null){
                    cache.close();
                    log.debug("Close abandoned PathChildrenCache[{}] successfully", o);
                }
            } catch (IOException e) {
                log.error("Close abandoned PathChildrenCache[{}] error", o, e);
            }
        });
    }


    /**
     * 组装服务路径：appPath/protocol(thrift/netty)/com.tiger.chaos.xxx_1.0.0(服务名)
     * @param referenceConfig
     */
    public String assembleServicePath(ReferenceConfig referenceConfig) {
        StringBuffer sb = new StringBuffer();
        //设置应用路径
        sb.append(appRegister.getAppPath());
        //服务地址: appPath/protocol(thrift/netty)/com.tiger.chaos.UserService_1.0.0(服务名)
        Class<?> enClosedClazz = referenceConfig.getInterfaceClass().getEnclosingClass();
        enClosedClazz = enClosedClazz == null ? referenceConfig.getInterfaceClass() : enClosedClazz;
        sb.append(Constants.PATH_SEPARATOR).append(referenceConfig.getProtocol()).append(Constants.PATH_SEPARATOR)
                .append(enClosedClazz.getName()).append(Constants.SERVICE_VERSION_SEPARATOR).append(referenceConfig.getVersion());
        return sb.toString();
    }

    /**
     * 组装服务提供者路径：appPath/protocol(thrift/netty)/com.tiger.chaos.xxx_1.0.0(服务名)/providers
     * @param referenceConfig
     */
    public String assembleProvidersPath(ReferenceConfig referenceConfig) {
        StringBuffer sb = new StringBuffer();
        //设置应用路径
        sb.append(appRegister.getAppPath());
        //服务地址: appPath/protocol(thrift/netty)/com.tiger.chaos.UserService_1.0.0(服务名)
        Class<?> enClosedClazz = referenceConfig.getInterfaceClass().getEnclosingClass();
        enClosedClazz = enClosedClazz == null ? referenceConfig.getInterfaceClass() : enClosedClazz;
        sb.append(Constants.PATH_SEPARATOR).append(referenceConfig.getProtocol()).append(Constants.PATH_SEPARATOR)
                .append(enClosedClazz.getName()).append(Constants.SERVICE_VERSION_SEPARATOR).append(referenceConfig.getVersion());
        //机器地址：servicePath/providers/
        sb.append(Constants.PATH_SEPARATOR).append("providers");
        return sb.toString();
    }

    /**
     * 组装服务应用者路径：appPath/protocol(thrift/netty)/com.tiger.chaos.xxx_1.0.0(服务名)/consumers/127.0.0.0
     * @param referenceConfig
     */
    public String assembleConsumerPath(ReferenceConfig referenceConfig) {
        StringBuffer sb = new StringBuffer();
        //设置应用路径
        sb.append(appRegister.getAppPath());
        //服务地址: appPath/protocol(thrift/netty)/com.tiger.chaos.UserService_1.0.0(服务名)
        Class<?> enClosedClazz = referenceConfig.getInterfaceClass().getEnclosingClass();
        enClosedClazz = enClosedClazz == null ? referenceConfig.getInterfaceClass() : enClosedClazz;
        sb.append(Constants.PATH_SEPARATOR).append(referenceConfig.getProtocol()).append(Constants.PATH_SEPARATOR)
                .append(enClosedClazz.getName()).append(Constants.SERVICE_VERSION_SEPARATOR).append(referenceConfig.getVersion());
        //机器地址：servicePath/consumers/127.0.0.0
        sb.append(Constants.PATH_SEPARATOR).append("consumers").append(Constants.PATH_SEPARATOR).append(referenceConfig.getHost());
        return sb.toString();
    }


    /**
     * 注销consumer和服务
     * @throws Exception
     */
    public void unDiscovery() throws Exception {
        if (!isRegistered) {
            //已注销不处理
            log.info("Services has unDiscovery");
            return;
        }
        //具体服务注销
        doUnRegister();

        if(CollectionUtils.isEmpty(this.referenceBeanMap.values())){
            log.info("No service to unDiscovery");
            return;
        }
        StringBuffer sb = new StringBuffer();
        for (ReferenceConfig config : this.referenceBeanMap.values()) {
            String consumerPath = assembleConsumerPath(config);
            try {
                //注销机器
                if(appRegister.getZkClient().checkExists().forPath(consumerPath) != null){
                    //自动注销时，可以先校验
                    appRegister.getZkClient().delete().forPath(consumerPath);
                }
            } catch (Exception e) {
                log.error("unDiscovery service[{}] provider[{}] error", config.getInterfaceName(), config.getHost(), e);
                throw e;
            }
        }

        isRegistered = false;
        //打印取消服务发现和注销消费者日志
        List<String> nameServiceList = referenceBeanMap == null || CollectionUtils.isEmpty(referenceBeanMap.values()) ? new ArrayList<>() :
                this.referenceBeanMap.values().stream().map(o -> {
                    Class<?> enClosedClazz = o.getInterfaceClass().getEnclosingClass();
                    enClosedClazz = enClosedClazz == null ? o.getInterfaceClass() : enClosedClazz;
                    return enClosedClazz.getName() + Constants.SERVICE_VERSION_SEPARATOR + o.getVersion();
                }).collect(Collectors.toList());
        log.debug("ServiceList[{}] undiscovered from zookeeper, removed consumer to zookeeper", StringUtils.join(nameServiceList, ","));
    }

    /**
     * 具体服务注销操作
     * @throws Exception
     */
    protected abstract void doUnRegister() throws Exception;

    /**
     * 移除连接监听器
     */
    protected void removeConnectionListener(){
        //移除监听器
        this.getAppRegister().getZkClient().getConnectionStateListenable().removeListener(zkConnectionListener);
    }

    public ReferenceRegister setAppRegister(ApplicationRegister appRegister) {
        Preconditions.checkArgument(appRegister != null, "appRegister can not be null");
        Preconditions.checkArgument(appRegister.isRegistered(), "Application must be registered before services");
        //将引入服务注册器回写
        appRegister.addReferenceRegister(this);
        this.appRegister = appRegister;
        return this;
    }

    public ApplicationRegister getAppRegister() {
        return appRegister;
    }

    /**
     * 导入新服务，已存在不做处理
     * @param referenceList
     * @return
     * @throws Exception
     */
    public ReferenceRegister importReferenceList(List<ReferenceConfig> referenceList) throws Exception {
        //1.过滤服务
        List<ReferenceConfig> protocolConfigs = doFilterServiceList(referenceList);
        if(CollectionUtils.isNotEmpty(protocolConfigs) && this.isRegistered){
            //存在协议服务并且已经加入referenceBeanMap了 & 该服务注册器已经注册了
            //2.注册consumer
            registerConsumer(protocolConfigs);

            //3.发现服务
            discoveryProviders(protocolConfigs);

            //4.具体发现服务
            doDiscovery();

            //5.填充新服务的代理
            doFillProxy(protocolConfigs);
        }
        return this;
    }

    /**
     * 设置代理
     * @param referenceList
     */
    protected abstract void doFillProxy(List<ReferenceConfig> referenceList);

    public Map<String, List<String>> getServiceProvidersMap() {
        return serviceProvidersMap;
    }

    public Map<String, ReferenceConfig> getReferenceBeanMap() {
        return referenceBeanMap;
    }

    public ReferenceRegister setReferenceBeanMap(Map<String, ReferenceConfig> referenceBeanMap) {
        this.referenceBeanMap = referenceBeanMap;
        return this;
    }

    public ProviderStrategy<String> getProviderStrategy() {
        return providerStrategy;
    }

    public ReferenceRegister setProviderStrategy(ProviderStrategy<String> providerStrategy) {
        this.providerStrategy = providerStrategy;
        return this;
    }

    /**
     * 获取服务名称列表(含版本号)
     * @return
     */
    public List<String> getServiceNameList() {
        return new ArrayList<>(serviceProvidersMap.keySet());
    }

    /**
     * 根据配置获取服务名称（service_1.0.0）
     * @param conf
     * @return
     */
    public abstract String getServiceNameByConf(ReferenceConfig conf);

    /**
     * 根据接口名称获取引入配置
     * @param interfaceName
     * @return
     */
    public ReferenceConfig getConfByInterfaceName(String interfaceName){
        return referenceBeanMap.get(interfaceName);
    }

    /**
     * 根据接口名称获取引入配置
     * @param clazz
     * @return
     */
    public ReferenceConfig getConfbyInterfaceClass(Class<?> clazz){
        return referenceBeanMap.get(clazz.getName());
    }

    public boolean isRegistered() {
        return isRegistered;
    }

}

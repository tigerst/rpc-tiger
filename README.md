# rpc通信框架 
## 一、背景  
   >本rpc框架主要用于数据中台的离线调度系统，致力与解决动态定向指定集群中的某台机器问题，并且兼容自动轮询策略。曾支持5w+的生产任务的正常运行。
    
## 二、项目结构  
  >rpc-tiger   
  >>rpc-common        
  >>rpc-thrift   
  >>rpc-netty   
  >>rpc-application-spring  
  >>rpc-thrift-spring 
  >>rpc-netty-spring  
  
  >模块说明   
  >>rpc-common: 封装rpc框架的主要处理逻辑。支持协议扩展，实现协议的不同部分即可。  
  >> 
  >>rpc-thrift: 基于thrift协议框架，定制化rpc功能。   
  >>
  >>rpc-netty: 基于netty通信框架 & protostuff序列化工具，实现方法同步调用功能。   
  >>
  >>rpc-application-spring: rpc应用注册spring化。   
  >>
  >>rpc-thrift-spring: thrift协议的rpc spring功能。   
  >>
  >>rpc-netty-spring: netty通信的rpc spring功能。   
  
## 三、主要功能  
  >1.该框架基于zookeeper注册中心，引用端可以自动发现zookeeper上的服务提供者。   
  >
  >2.目前支持thrift & netty两种远程调用方式，并支持对服务的重试。   
  >
  >3.对thrift & netty等服务进行检测。对于异常情况，可以自动拉起。    
  >
  >4.通过外部实现SyncMachineService接口，能达到对机器和服务的信息同步，实时监测机器和服务详情。   
  >
  >5.通过外部实现NoticeService接口，可以在服务出现异常时通知系统管理员。   
  >
  >6.zk上服务提供者监控task，将会监控服务的提供着，并对服务提供者小于指定阈值时，进行告警。
  
## 四、整体架构 & 主要功能图解     
  >![RPC架构V1](https://user-images.githubusercontent.com/19148139/114145569-2063b600-9949-11eb-9cdc-9da25519e110.png)

## 五、用法案例(以netty为例，thrift用法类似)
  >使用之前引入响应的jar包，rpc-common为必须引入的。netty/thrfit分别引入各自模块的jar。   
  >
  >1 创建应用对象
  >>1.1 zookeeper配置组装，zkClient创建（多个应用时，可以使用同一个）
  >>>          private ZkConfig assembleZkConfig() {
  >>>              return ZkConfig.builder().namespace(namespace).zkServers(zkServers).sessionTimeOut(sessionTimeOut)
  >>>                      .connectionTimeOut(connectionTimeOut).retryInterval(retryInterval).retryTimes(retryTimes).build();
  >>>          }
  >>>      
  >>>          @Bean("zkClientNoNs")
  >>>          public CuratorFramework zkClientNoNs(){
  >>>              return ZkUtils.createZkClientWithoutNameSpace(assembleZkConfig());
  >>>          }
  >>1.2 引入应用创建（如果不传入zkClientNoNs，会根据zookeeper配置自动创建）
  >>>       @Bean("monitorApplication")
  >>>       public ApplicationBean monitorApplication(@Autowired @Qualifier("zkClientNoNs") CuratorFramework zkClientNoNs) throws Exception {
  >>>           ApplicationConfig appConfig = new ApplicationConfig();
  >>>           appConfig.setName(ApplicationType.MONITOR.getValue()).setGroup(appGroup).setOwner(monitorAppOwner)
  >>>                   .setEnv(appEnv).setCluster(appCluster);
  >>>           ApplicationBean register = new ApplicationBean(assembleZkConfig(), zkClientNoNs, appConfig);
  >>>           return register;
  >>>       }
  >>1.3 服务应用创建（如果不传入zkClientNoNs，会根据zookeeper配置自动创建）
  >>>       @Bean("userApplication")
  >>>       public ApplicationBean workerApplication(@Autowired @Qualifier("zkClientNoNs") CuratorFramework zkClientNoNs, @Autowired SyncMachineService syncMachineService) throws Exception {
  >>>           ApplicationConfig appConfig = new ApplicationConfig();
  >>>           appConfig.setName(ApplicationType.USER.getValue()).setGroup(appGroup).setOwner(userAppOwner)
  >>>                  .setEnv(appEnv).setCluster(appCluster);
  >>>
  >>>           MonitorConfig monitorConfig = new MonitorConfig();
  >>>           monitorConfig.setSyncInitialDelay(syncInitialDelay).setSyncDelay(syncDelay).setSyncTimeUnit(syncTimeUnit)
  >>>                   .setServiceMonitorInitialDelay(serviceMonitorInitialDelay).setServiceMonitorDelay(serviceMonitorDelay)
  >>>                  .setServiceMonitorTimeUnit(serviceMonitorTimeUnit).setAutoProcessServiceThread(isAutoProcessServiceThread)
  >>>                   .setZkProviderMonitorInitialDelay(zkProviderMonitorInitialDelay).setZkProviderMonitorDelay(zkProviderMonitorDelay)
  >>>                  .setZkProviderMonitorTimeUnit(zkProviderMonitorTimeUnit).setProvidersThreshold(providersThreshold);
  >>>  
  >>>           return new ApplicationBean(assembleZkConfig(), zkClientNoNs, appConfig, monitorConfig, null, syncMachineService);
  >>>      }
  >2 服务暴露注册器创建
  >>>      @Bean("nettyRegistry")
  >>>      public NettyServiceBeanRegister nettyRegistry(@Autowired @Qualifier("userApplication") ApplicationRegister userApplication) throws Exception {
  >>>           List<ServiceConfig> serviceConfigList = new ArrayList<ServiceConfig>();
  >>>           fillServiceList(serviceConfigList);
  >>>           NettyServiceBeanRegister registry = new NettyServiceBeanRegister(userApplication, serviceConfigList,
  >>>                rpcSelectorThreads, rpcWorkerThreads, serverPort);
  >>>           return registry;
  >>>      }
  >>>      private void fillServiceList(List<ServiceConfig> serviceConfigList){
  >>>           ServiceConfig<DtAccountService> dtAccountServiceConfig = new ServiceConfig<DtAccountService>();
  >>>           dtAccountServiceConfig.setRef(dtAccountService).setProtocol(ProtocolTypeEnum.NETTY.getValue())
  >>>                .setUseHostName(useHostName).setInterfaceClass(DtAccountService.class);
  >>>           serviceConfigList.add(dtAccountServiceConfig);
  >>>      }
  >3 引入应用服务
  >>3.1 创建连接池
  >>>      @Bean("nettyPool")
  >>>      public GenericKeyedObjectPool<String, NSocket> nettyPool(){
  >>>           GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
  >>>           config.setMaxTotal(maxTotal);
  >>>           config.setMaxWaitMillis(maxWait);
  >>>           config.setTestOnBorrow(true);
  >>>           config.setTestOnReturn(false);
  >>>           config.setJmxEnabled(false);
  >>>           config.setTestWhileIdle(true);
  >>>           config.setBlockWhenExhausted(true);
  >>>           config.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
  >>>           GenericKeyedObjectPool<String, NSocket> pool = new GenericKeyedObjectPool<String, NSocket>(
  >>>               NettySocketPoolFactory.builder().timeout(timeout).build(), config);
  >>>           return pool;
  >>>      }
  >>3.2 创建服务发现器
  >>>      @Bean("monitorDiscoveryNetty")
  >>>      public NettyServiceBeanDiscovery discovery(@Autowired @Qualifier("nettyPool") GenericKeyedObjectPool<String, NSocket> pool,
  >>>                                                         @Autowired @Qualifier("monitorApplication")ApplicationRegister monitorApplication) throws Exception {
  >>>           List<ReferenceConfig> referenceList = new ArrayList<ReferenceConfig>();
  >>>           fillReferenceList(referenceList);
  >>>           NettyServiceBeanDiscovery discovery = new NettyServiceBeanDiscovery(monitorApplication, referenceList, pool);
  >>>           return discovery;
  >>>      }
  >>>      private void fillReferenceList(List<ReferenceConfig> referenceList) {
  >>>           ReferenceConfig<DtSyncMachineService> syncMachineConfig = new ReferenceConfig<DtSyncMachineService>();
  >>>           ((ReferenceConfig)syncMachineConfig.setProtocol(ProtocolTypeEnum.NETTY.getValue())
  >>>               .setUseHostName(useHostName).setInterfaceClass(DtSyncMachineService.class)).setRetry(0);
  >>>           referenceList.add(syncMachineConfig);
  >>>      }
  >>3.2 从工厂中获取实例对象
  >>>      @Bean
  >>>      public DtSyncMachineService dtSyncMachineService(@Autowired @Qualifier("monitorDiscoveryNetty") NettyServiceBeanDiscovery discovery) throws Exception {
  >>>           NettyReferenceProxyFactoryBean<DtSyncMachineService> factoryBean = new NettyReferenceProxyFactoryBean<DtSyncMachineService>();
  >>>           factoryBean.setDiscovery(discovery);
  >>>           factoryBean.setIFaceInterface(DtSyncMachineService.class);
  >>>           return factoryBean.getObject();
  >>>      }
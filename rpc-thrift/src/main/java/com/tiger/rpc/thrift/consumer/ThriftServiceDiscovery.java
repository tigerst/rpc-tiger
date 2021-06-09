package com.tiger.rpc.thrift.consumer;

import com.tiger.rpc.common.config.ReferenceConfig;
import com.tiger.rpc.common.consumer.policy.ProviderStrategy;
import com.tiger.rpc.common.consumer.policy.RoundRobinStrategy;
import com.tiger.rpc.common.enums.ProtocolTypeEnum;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.common.register.ReferenceRegister;
import com.tiger.rpc.common.utils.Constants;
import com.tiger.rpc.thrift.consumer.handler.ThriftDefaultHandler;
import com.tiger.rpc.thrift.utils.ThriftUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.thrift.transport.TSocket;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * @ClassName: ThriftServiceDiscovery.java
 *
 * @Description: thrift服务发现
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/18
 */
@Slf4j
public class ThriftServiceDiscovery extends ReferenceRegister {

	/**
	 * thrift连接池
	 */
	private GenericKeyedObjectPool<String, TSocket> pool;

	public ThriftServiceDiscovery(){
		super();
	}

	public ThriftServiceDiscovery(ApplicationRegister appRegister, List<ReferenceConfig> referenceList){
	    this(appRegister, referenceList, new RoundRobinStrategy(), null);
	}

	public ThriftServiceDiscovery(ApplicationRegister appRegister, List<ReferenceConfig> referenceList, GenericKeyedObjectPool<String, TSocket> pool){
		this(appRegister, referenceList, new RoundRobinStrategy(), pool);
	}

	public ThriftServiceDiscovery(ApplicationRegister appRegister, List<ReferenceConfig> referenceList, ProviderStrategy<String> providerStrategy, GenericKeyedObjectPool<String, TSocket> pool){
        Preconditions.checkArgument(appRegister != null, "appRegister can not be null");
        Preconditions.checkArgument(appRegister.isRegistered(), "Application must be registered before references");
        super.setAppRegister(appRegister);
        //过滤服务
        doFilterServiceList(referenceList);
		//if pool equals null, then use default pool
		pool = pool == null ? getDefaultPool() : pool;
		this.pool = pool;
        //填充代理类: 需要使用线程池
        doFillProxy(Lists.newArrayList(super.getReferenceBeanMap().values()));
        //将引入服务注册器回写
        super.getAppRegister().addReferenceRegister(this);
        super.setProviderStrategy(providerStrategy);
	}

	@Override
	protected List<ReferenceConfig> doFilterServiceList(List<ReferenceConfig> referenceList) {
		List<ReferenceConfig> protocolConfigs = Lists.newArrayList();
		if(referenceList != null){
			//存在的话，只需要thrift协议服务
			referenceList.stream().forEach(o -> {
				if(ProtocolTypeEnum.THRIFT.getValue().equals(o.getProtocol())){
					//接口(xxx.Iface)<--->ReferenceConfig
					if(super.getReferenceBeanMap().putIfAbsent(o.getInterfaceName(), o) == null){
						//不存在时，加入；已存在跳过
						protocolConfigs.add(o);
					}
				}
			});
		}
		return protocolConfigs;
	}

	@Override
	protected void doDiscovery() throws Exception {
		//此处不做处理
	}


	@Override
	public void unDiscovery() throws Exception {
		//注销可以被注销的服务
		super.unDiscovery();
	}

	@Override
	protected void doUnRegister() throws Exception {
		//此处不做处理
	}

	/**
	 * 填充代理
	 * @param referenceList
	 */
	@Override
	protected void doFillProxy(List<ReferenceConfig> referenceList) {
		if(CollectionUtils.isEmpty(referenceList)){
			return;
		}
		Preconditions.checkArgument(pool != null, "pool can not be null");
		ThriftDefaultHandler handler;
		Object proxy;
		/**
		 * 此处不抛异常
		 */
		for (ReferenceConfig config : referenceList) {
			try {
				//填充默认代理
				handler = new ThriftDefaultHandler(this);
				//设置连接池
				handler.setPool(pool);
				//设置服务版本号
				handler.setServiceVersion(config.getVersion());
				//设置服务工厂方法
				handler.setClientFactory(ThriftUtils.getClientFactory(config.getInterfaceClass()));
				if(config.getRetry() > 0){
					//设置重试次数
					handler.setRetry(config.getRetry());
				}
				//获取类加载器
//				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				ClassLoader classLoader = config.getInterfaceClass().getClassLoader();
				//创建jdk反射代理实例
				proxy = Proxy.newProxyInstance(classLoader, new Class[] { config.getInterfaceClass() }, handler);
				//回写代理
				config.setProxy(proxy);
			} catch (Exception e) {
				log.error("Fill class[{}] proxy error", config, e);
			}
		}

	}

	@Override
	public String getServiceNameByConf(ReferenceConfig conf) {
		StringBuffer sb = new StringBuffer();
		//组装服务路径(service_1.0.0)，此处为接口的外层类名
		Class<?> enClosedClazz = conf.getInterfaceClass().getEnclosingClass();
		enClosedClazz = enClosedClazz == null? conf.getInterfaceClass() : enClosedClazz;
		sb.append(enClosedClazz.getName()).append(Constants.SERVICE_VERSION_SEPARATOR).append(conf.getVersion());
		return sb.toString();
	}

	@Override
	public void close() throws IOException {
		try {
			log.info("Start to close class[{}]...", this.getClass().getName());
			super.unDiscovery();
			log.info("Class[{}] closed successfully", this.getClass().getName());
		} catch (Exception e) {
			log.error("Close class[{}] error", this.getClass().getName(), e);
		}
	}

	public GenericKeyedObjectPool<String, TSocket> getPool() {
		return pool;
	}

	public ThriftServiceDiscovery setPool(GenericKeyedObjectPool<String, TSocket> pool) {
		this.pool = pool;
		return this;
	}

	/**
	 * 静态方法：获取默认配置的线程池
	 * @return
	 */
	private static GenericKeyedObjectPool<String, TSocket> getDefaultPool() {
		//连接池配置
		GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
		/**
		 * 连接池最大连接数
		 */
		config.setMaxTotal(8);
		//获取连接的最大等待时间
		config.setMaxWaitMillis(60000);
		//申请连接检测：true
		config.setTestOnBorrow(true);
		//归还连接检测：false
		config.setTestOnReturn(false);
		//是否开启jmx：true
		config.setJmxEnabled(false);
		//是否超时检测：true
		config.setTestWhileIdle(true);
		//阻塞连接：最大阻塞时间为maxTotal
		config.setBlockWhenExhausted(true);
		//空闲对象检测线程的执行周期，即多长时候执行一次空闲对象检测
		config.setTimeBetweenEvictionRunsMillis(30000);
		GenericKeyedObjectPool<String, TSocket> pool = new GenericKeyedObjectPool<String, TSocket>(
				ThriftSocketPoolFactory.builder().timeout(60000).build(), config);
		return pool;
	}

}

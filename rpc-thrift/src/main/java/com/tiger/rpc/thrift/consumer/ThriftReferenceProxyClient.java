package com.tiger.rpc.thrift.consumer;

import com.google.common.collect.Lists;
import com.tiger.rpc.common.config.ReferenceConfig;
import com.tiger.rpc.common.enums.ProtocolTypeEnum;
import com.tiger.rpc.common.enums.ServiceCodeEnum;
import com.tiger.rpc.common.exception.ServiceException;
import com.tiger.rpc.common.utils.Constants;
import com.tiger.rpc.thrift.consumer.handler.ThriftDefaultHandler;
import com.tiger.rpc.thrift.consumer.handler.ThriftDirectorHandler;
import com.tiger.rpc.thrift.utils.ThriftUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: ThriftDefaultProxy.java
 *
 * @Description: consumer默认代理，内部管理有连接池，代理销毁时，清空资源
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Slf4j
public class ThriftReferenceProxyClient {

	/**
	 * 引入发现服务
	 */
	private ThriftServiceDiscovery discovery;

	/**
	 * 代理执行，动态获取服务provider
	 * 获取代理处理器
	 * @param iFaceInterface
	 * @return
	 */
	public <T> T getProxy(Class<T> iFaceInterface) throws ServiceException {
		if(iFaceInterface == null){
			throw new ServiceException(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getCode(),
					String.format(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getValue(), "iFaceInterface"));
		}
		//校验discovery
		checkDiscovery(discovery);
		//获取接口配置
		ReferenceConfig config = discovery.getConfbyInterfaceClass(iFaceInterface);
		if(config == null){
			//接口未引入
			throw new ServiceException(ServiceCodeEnum.INTERFACE_NOT_IMPORT.getCode(),
					String.format(ServiceCodeEnum.INTERFACE_NOT_IMPORT.getValue(), iFaceInterface.getName()));
		}
		if(config.getProxy() != null){
			return (T) config.getProxy();
		}
		//使用服务发现器构造处理器
		ThriftDefaultHandler handler = new ThriftDefaultHandler(discovery);
		//设置连接池
		handler.setPool(this.discovery.getPool());
		//设置服务版本号
		handler.setServiceVersion(config.getVersion());
		//设置clientFactor
		handler.setClientFactory(ThriftUtils.getClientFactory(iFaceInterface));
		if(config.getRetry() > 0){
			//设置重试次数
			handler.setRetry(config.getRetry());
		}
		//获取类加载器
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		//创建代理实例
		T proxy = (T) Proxy.newProxyInstance(classLoader, new Class[] { iFaceInterface }, handler);
		//反填代理
		config.setProxy(proxy);
		return proxy;
	}

	/**
	 * 代理执行，动态获取直连代理
	 * 不做缓存，使用时创建，使用后销毁
	 * @param iFaceInterface    接口方法
	 * @param host  指定服务器
	 * @param <T>	泛型
	 * @return
	 * @throws ServiceException
	 */
	public <T> T getDirectorProxy(Class<T> iFaceInterface, String host) throws ServiceException {
		return this.getDirectorProxy(iFaceInterface, host, null);
	}

	/**
	 * 代理执行，动态获取直连代理
	 * 不做缓存，使用时创建，使用后销毁
	 * @param iFaceInterface	接口方法
	 * @param host	指定服务器
	 * @param port	指定端口
	 * @param <T>
	 * @return
	 * @throws ServiceException
	 */
	public <T> T getDirectorProxy(Class<T> iFaceInterface, String host, Integer port) throws ServiceException {
		return getDirectorProxy(iFaceInterface, Lists.newArrayList(host + Constants.HOST_PORT_SEPARATOR + String.valueOf(port)));
	}



	/**
	 * 基于应用的代理执行，动态获取直连代理
	 * 不做缓存，使用时创建，使用后销毁
	 * @param iFaceInterface	接口方法
	 * @param hostPorts	host:port列表
	 * @param <T>
	 * @return
	 * @throws ServiceException
	 */
	public <T> T getDirectorProxy(Class<T> iFaceInterface, List<String> hostPorts) throws ServiceException {
		if(iFaceInterface == null){
			throw new ServiceException(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getCode(),
					String.format(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getValue(), "iFaceInterface"));
		}
		if (CollectionUtils.isEmpty(hostPorts)) {
			throw new ServiceException(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getCode(),
					String.format(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getValue(), "hostPorts"));
		}
		//校验discovery
		checkDiscovery(discovery);
		//获取接口配置
		ReferenceConfig config = discovery.getConfbyInterfaceClass(iFaceInterface);
		if(config == null){
			//接口未引入
			throw new ServiceException(ServiceCodeEnum.INTERFACE_NOT_IMPORT.getCode(),
					String.format(ServiceCodeEnum.INTERFACE_NOT_IMPORT.getValue(), iFaceInterface.getName()));
		}

		//初始化基于服务发现器的直连代理
		ThriftDirectorHandler handler = new ThriftDirectorHandler(discovery);
		//设置连接池
		handler.setPool(this.discovery.getPool());
		handler.setServiceVersion(config.getVersion());
		handler.setClientFactory(ThriftUtils.getClientFactory(iFaceInterface));
		if(config.getRetry() >= 0){
			//设置重试次数
			handler.setRetry(config.getRetry());
		}
		//设置小集群地址
		handler.setUris(getUris(hostPorts));
		//获取类加载器
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		//创建代理实例，强转类型
		return (T) Proxy.newProxyInstance(classLoader, new Class[] { iFaceInterface }, handler);
	}

	/**
	 * 校验服务发现器
	 * @param discovery
	 */
	private void checkDiscovery(ThriftServiceDiscovery discovery) {
		//校验代理
		if(this.discovery == null){
			//服务发现器校验
			throw new ServiceException(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getCode(),
					String.format(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getValue(), "discovery"));
		}
		if(this.discovery.getPool() == null){
			//连接池校验
			throw new ServiceException(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getCode(),
					String.format(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getValue(), "pool"));
		}
		if(!this.discovery.isRegistered()){
			//服务发现器为开启
			throw new ServiceException(ServiceCodeEnum.DISCOVERY_NOT_INITIALIZED.getCode(),
					ServiceCodeEnum.DISCOVERY_NOT_INITIALIZED.getValue());
		}
	}

	/**
	 * 根据传入的host:port列表，获取uris
	 * @param hostPorts
	 * @return
	 */
	private List<String> getUris(List<String> hostPorts) {
		List<String> uris = new ArrayList<>();
		StringBuffer sb = new StringBuffer();
		String[] elements;
		String portRegex = Constants.LINE_START_REGEX + Constants.PORT_VALUE_REGEX;
		for (String hostPort : hostPorts) {
			if (StringUtils.isBlank(hostPort)) {
				//跳过为空的
				continue;
			}
			elements = hostPort.split(Constants.HOST_PORT_SEPARATOR);
			if (elements.length != 2) {
				//跳过不规范的
				continue;
			}
			if (StringUtils.isBlank(elements[0])) {
				throw new ServiceException(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getCode(),
						String.format(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getValue(), "host"));
			}
			//null不检测，从大集群中匹配端口
			if (!Constants.NULL_STR.equalsIgnoreCase(elements[1].trim()) && elements[1].trim().matches(portRegex)) {
				throw new ServiceException(ServiceCodeEnum.ILLEGAL_PARAMETER.getCode(),
						String.format(ServiceCodeEnum.ILLEGAL_PARAMETER.getValue(), "port"));
			}
			sb.setLength(0);
			//拼接uri---> thrift://host:port or thrift://host:null
			sb.append(ProtocolTypeEnum.THRIFT.getValue()).append(Constants.PROTOCOL_HOST_SEPARATOR)
					.append(hostPort);
			uris.add(sb.toString());
		}
		return uris;
	}

}

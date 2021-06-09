package com.tiger.rpc.netty.consumer;

import com.google.common.collect.Lists;
import com.tiger.rpc.common.consumer.policy.ProviderStrategy;
import com.tiger.rpc.common.enums.ProtocolTypeEnum;
import com.tiger.rpc.common.enums.ServiceCodeEnum;
import com.tiger.rpc.common.exception.ServiceException;
import com.tiger.rpc.common.utils.Constants;
import com.tiger.rpc.common.utils.UriUtils;
import com.tiger.rpc.netty.consumer.handler.NettyDirectorHandler;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;

import java.lang.reflect.Proxy;
import java.util.List;

/**
 * @ClassName: NettyDirectorProxyClient.java
 *
 * @Description: consumer直连代理，内部管理有连接池，代理销毁时，清空资源
 *
 * @Author: Tiger
 *
 * @Date: 2021/5/7
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Slf4j
public class NettyDirectorProxyClient {

	/**
	 * 引入连接池
	 */
	private GenericKeyedObjectPool<String, NSocket> pool;

	/**
	 * 选择策略
	 */
	private ProviderStrategy<String> providerStrategy;

	/**
	 * 重试次数，默认不重试
	 */
	private int retry = 0;

	/**
	 * 不基于应用的直连代理执行
	 * 不做缓存，使用时创建，使用后销毁
	 * @param iFaceInterface	接口方法
	 * @param host	指定服务器
	 * @param port	指定端口
	 * @param <T>
	 * @return
	 * @throws ServiceException
	 */
	public <T> T getProxy(Class<T> iFaceInterface, String host, Integer port) throws ServiceException {
		return getProxy(iFaceInterface, host, port, this.retry);
	}

	/**
	 * 不基于应用的直连代理执行
	 * 不做缓存，使用时创建，使用后销毁
	 * @param iFaceInterface	接口方法
	 * @param host	指定服务器
	 * @param port	指定端口
	 * @param retry	重试次数
	 * @param <T>
	 * @return
	 * @throws ServiceException
	 */
	public <T> T getProxy(Class<T> iFaceInterface, String host, Integer port, int retry) throws ServiceException {
		//直接调用小集群的方法处理
		return getProxy(iFaceInterface, Lists.newArrayList(host+Constants.HOST_PORT_SEPARATOR+port), retry);
	}

	/**
	 * 不基于应用的直连代理执行
	 * 不做缓存，使用时创建，使用后销毁
	 * @param iFaceInterface	接口方法
	 * @param hostPorts	host:port列表
	 * @param <T>
	 * @return
	 * @throws ServiceException
	 */
	public <T> T getProxy(Class<T> iFaceInterface, List<String> hostPorts) throws ServiceException {
		return getProxy(iFaceInterface, hostPorts, this.retry);
	}

	/**
	 * 不基于应用的直连代理执行
	 * 不做缓存，使用时创建，使用后销毁
	 * @param iFaceInterface	接口方法
	 * @param hostPorts	host:port列表
	 * @param retry	重试次数
	 * @param <T>
	 * @return
	 * @throws ServiceException
	 */
	public <T> T getProxy(Class<T> iFaceInterface, List<String> hostPorts, int retry) throws ServiceException {
		if(iFaceInterface == null){
			throw new ServiceException(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getCode(),
					String.format(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getValue(), "iFaceInterface"));
		}
		if (CollectionUtils.isEmpty(hostPorts)) {
			throw new ServiceException(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getCode(),
					String.format(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getValue(), "hostPorts"));
		}
		//小集群地址解析 & 检测
		List<String> uris = UriUtils.getUrisNotNullPort(hostPorts, ProtocolTypeEnum.NETTY.getValue());
		if (CollectionUtils.isEmpty(uris)) {
			throw new ServiceException(ServiceCodeEnum.ILLEGAL_PARAMETER.getCode(),
					String.format(ServiceCodeEnum.ILLEGAL_PARAMETER.getValue(), "hostPorts"));
		}
		//初始化直连代理(socket连接池必须传入)
		NettyDirectorHandler handler = new NettyDirectorHandler(pool);
		//传入策略
		handler.setProviderStrategy(providerStrategy);
		handler.setRetry(retry);
		//设置小集群地址
		handler.setUris(uris);

		//获取类加载器
//		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader classLoader = iFaceInterface.getClassLoader();
		//创建代理实例，强转类型
		return (T) Proxy.newProxyInstance(classLoader, new Class[] { iFaceInterface }, handler);
	}

}

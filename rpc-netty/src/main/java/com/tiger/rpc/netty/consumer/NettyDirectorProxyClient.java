package com.tiger.rpc.netty.consumer;

import com.tiger.rpc.common.enums.ProtocolTypeEnum;
import com.tiger.rpc.common.enums.ServiceCodeEnum;
import com.tiger.rpc.common.exception.ServiceException;
import com.tiger.rpc.common.utils.Constants;
import com.tiger.rpc.netty.consumer.handler.NettyDirectorHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;

import java.lang.reflect.Proxy;

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
		if(iFaceInterface == null){
			throw new ServiceException(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getCode(),
					String.format(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getValue(), "iFaceInterface"));
		}
		if (StringUtils.isBlank(host)) {
			throw new ServiceException(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getCode(),
					String.format(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getValue(), "host"));
		}
		if (port == null) {
			throw new ServiceException(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getCode(),
					String.format(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getValue(), "port"));
		}
		if (port.intValue() < 1024 || port.intValue() > 65535) {
			throw new ServiceException(ServiceCodeEnum.ILLEGAL_PARAMETER.getCode(),
					String.format(ServiceCodeEnum.ILLEGAL_PARAMETER.getValue(), "port") + ", 必须在1024～65535范围内");
		}
		//初始化直连代理(socket连接池必须传入)
		NettyDirectorHandler handler = new NettyDirectorHandler(pool);
		handler.setRetry(retry);
		StringBuffer sb = new StringBuffer();
		//拼接uri---> netty://host:port
		sb.append(ProtocolTypeEnum.NETTY.getValue()).append(Constants.PROTOCOL_HOST_SEPARATOR)
				.append(host).append(Constants.HOST_PORT_SEPARATOR).append(port);

		handler.setUri(sb.toString());
		//获取类加载器
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		//创建代理实例，强转类型
		return (T) Proxy.newProxyInstance(classLoader, new Class[] { iFaceInterface }, handler);
	}

}

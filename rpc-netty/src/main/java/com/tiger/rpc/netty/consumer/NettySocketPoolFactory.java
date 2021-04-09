package com.tiger.rpc.netty.consumer;

import com.alibaba.fastjson.JSON;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * @ClassName: NettySocketPoolFactory.java
 *
 * @Description: Netty tSocket工具
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/30
 */
@Data
@Builder( toBuilder = true)
@Slf4j
public class NettySocketPoolFactory extends BaseKeyedPooledObjectFactory<String, NSocket> {

	/**
	 * tSocket耗时设置
	 */
	private Integer timeout;

	/**
	 * HOST:PORT分割符
	 */
	private final String HOST_PORT_SEPARATOR = ":";

	@Override
	public NSocket create(String key) throws Exception {
		log.debug("Start to create TSocket by key[{}]", key);
		//创建连接
		String[] str = key.split(HOST_PORT_SEPARATOR);
		String host = str[0];
		Integer port = Integer.valueOf(str[1]);
		NSocket nsocket = null;
		try {
			//创建连接
			if(timeout != null && timeout > 0){
				nsocket = new NSocket(host, port, timeout);
			} else {
				nsocket = new NSocket(host, port);
			}
			//打开连接
			nsocket.open();
		} catch (Exception e){
			//先打印，在抛出异常
			log.error("Create TSocket[{}] of key[{}] error: {}", nsocket != null ? JSON.toJSONString(nsocket.getLocalSocketAddress()) : null, key, e);
			throw e;
		}
		log.debug("Create TSocket[{}] of key[{}] successfully", JSON.toJSONString(nsocket.getLocalSocketAddress()), key);
		return nsocket;
	}

	@Override
	public PooledObject<NSocket> wrap(NSocket value) {
		//绑定连接
		return new DefaultPooledObject<>(value);
	}

	@Override
	public void destroyObject(String key, PooledObject<NSocket> p) throws Exception {
		//先缓存序列化，否则内部重置为null，调用不到
		String tSocket = JSON.toJSONString(p.getObject().getLocalSocketAddress());
		log.debug("Start to destroy TSocket[{}] of key[{}]", tSocket, key);
		//关闭连接
		if(p != null && p.getObject() != null && p.getObject().isOpen()){
			p.getObject().close();
		}
		log.debug("Destroy TSocket[{}] of key[{}] successfully", tSocket, key);
	}

	@Override
	public boolean validateObject(String key, PooledObject<NSocket> p) {
		//检验连接
		String tSocket = JSON.toJSONString(p.getObject().getLocalSocketAddress());
		log.debug("Start to check TSocket[{}] of key[{}]", tSocket, key);
		boolean isValid = false;
		if(p != null && p.getObject() != null){
			isValid = p.getObject().isOpen();
		}
		log.debug("Check TSocket[{}] of key[{}] result[{}]", tSocket, key, isValid);
		return isValid;
	}

}

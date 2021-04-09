package com.tiger.rpc.thrift.consumer;

import com.alibaba.fastjson.JSON;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.transport.TSocket;

/**
 * @ClassName: ThriftSocketPoolFactory.java
 *
 * @Description: thrift socket工具
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/18
 */
@Data
@Builder( toBuilder = true)
@Slf4j
public class ThriftSocketPoolFactory extends BaseKeyedPooledObjectFactory<String, TSocket> {

	/**
	 * socket耗时设置
	 */
	private Integer timeout;

	/**
	 * HOST:PORT分割符
	 */
	private final String HOST_PORT_SEPARATOR = ":";

	@Override
	public TSocket create(String key) throws Exception {
		log.debug("Start to create TSocket by key[{}]", key);
		//创建连接
		String[] str = key.split(HOST_PORT_SEPARATOR);
		String host = str[0];
		Integer port = Integer.valueOf(str[1]);
		TSocket tsocket = null;
		try {
			//创建连接
			if(timeout != null && timeout > 0){
				tsocket = new TSocket(host, port, timeout);
			} else {
				tsocket = new TSocket(host, port);
			}
			//打开连接
			tsocket.open();
		} catch (Exception e){
			//先打印，在抛出异常
			log.error("Create TSocket[{}] of key[{}] error: {}", JSON.toJSONString(tsocket.getSocket().getLocalSocketAddress()), key, e);
			throw e;
		}
		log.debug("Create TSocket[{}] of key[{}] successfully", JSON.toJSONString(tsocket.getSocket().getLocalSocketAddress()), key);
		return tsocket;
	}

	@Override
	public PooledObject<TSocket> wrap(TSocket value) {
		//绑定连接
		return new DefaultPooledObject<>(value);
	}

	@Override
	public void destroyObject(String key, PooledObject<TSocket> p) throws Exception {
		String tSocket = JSON.toJSONString(p.getObject().getSocket().getLocalSocketAddress());
		log.debug("start to destroy TSocket[{}] of key[{}]", tSocket, key);
		//关闭连接
		if(p != null && p.getObject() != null && p.getObject().isOpen()){
			p.getObject().close();
		}
		log.debug("Destroy socket[{}] of key[{}] successfully", tSocket, key);
	}

	@Override
	public boolean validateObject(String key, PooledObject<TSocket> p) {
		//检验连接
		String tSocket = JSON.toJSONString(p.getObject().getSocket().getLocalSocketAddress());
		log.debug("Start to check TSocket[{}] of key[{}]", tSocket, key);
		boolean isValid = false;
		if(p != null && p.getObject() != null){
			isValid = p.getObject().isOpen();
		}
		log.debug("Check TSocket[{}] of key[{}] result[{}]", tSocket, key, isValid);
		return isValid;
	}

}

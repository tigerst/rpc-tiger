package com.tiger.rpc.netty.provider;

import com.alibaba.fastjson.JSON;
import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.common.enums.ProtocolTypeEnum;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.common.register.ServiceRegister;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

/**
 * @ClassName: NettyServiceRegister.java
 *
 * @Description: 注册服务提供者<br>
 * 				使用{@link java.util.Map}发布多个服务<br>
 * 				格式:<br>
 * 				|rpc<br>
 * 				|- com.tiger.chaos.UserService_1.1.1<br>
 * 				|-- 192.168.1.21:8089:3<br>
 * 				|-- 192.168.1.22:8089:2
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/30
 */
@Slf4j
public class NettyServiceRegister extends ServiceRegister {

	/**
	 * 服务端进程，静态常量，只保存一份
	 */
	private static NettyServer nettyServer;

	/**
	 * selector线程数
	 */
	private int selectorThreads;

	/**
	 * worker线程数
	 */
	private int workerThreads;

	public NettyServiceRegister(ApplicationRegister appRegister, List<ServiceConfig> serviceList, int selectorThreads, int workerThreads, int serverPort) {
		super(appRegister, serviceList, serverPort);
		this.workerThreads = workerThreads;
	}

	@Override
	public ProtocolTypeEnum getProtocolType() {
		//响应Netty协议
		return ProtocolTypeEnum.NETTY;
	}

	/**
	 * 构建函数之后，注册Netty服务
	 * @throws Exception
	 */
	@Override
	public void doRegister() throws Exception {
		//注册具体的应用，启动Netty线程
		if(this.isProtocolServiceServing()){
			return;
		}
		if(nettyServer == null){
			//线程不存在时，直接创建线程
			nettyServer = new NettyServer(this.getClass().getSimpleName() + "_NettyServer",
					super.getServiceBeanMap(), super.getServerPort(), selectorThreads, workerThreads);
			log.debug("NettyServer[{}] created. ", nettyServer.getName());
		} else if (!nettyServer.isServing()){
			//不在服务时，直接启动服务
			log.debug("Server exists, alive but not serving, and will be started. ");
		}
		//启动线程
		nettyServer.start();
		log.debug("NettyServer[{}] started", nettyServer.getName());
	}

	@Override
	public boolean isProtocolServiceServing() {
		if(nettyServer != null && nettyServer.isServing()){
			//服务，打印日志，返回服务正常运行
			log.debug("NettyServer[{}] is serving", nettyServer.getName());
			return true;
		}
		log.debug("Service error, NettyServer[{}] . ", JSON.toJSONString(nettyServer));
		return false;
	}

	/**
	 * jdk对象销毁时，注销Netty服务
	 * @throws Exception
	 */
	@Override
	public void doUnRegister() throws Exception {
		//关闭Netty服务线程
		log.debug("Start to doUnRegister, and will stop nettyServer");
		if (nettyServer != null) {
			nettyServer.stopServer();
		}
		log.debug("DoUnRegister successfully");
	}

	@Override
	public void close() throws IOException {
		try {
			log.info("Start to close class[{}]...", this.getClass().getName());
			super.unRegister();
			log.info("class[{}] closed successfully", this.getClass().getName());
		} catch (Exception e) {
			log.error("Close class[{}] error" , this.getClass().getName(), e);
		}
	}

	public int getWorkerThreads() {
		return workerThreads;
	}

	public NettyServiceRegister setWorkerThreads(int workerThreads) {
		this.workerThreads = workerThreads;
		return this;
	}

	/**
	 * 覆盖方法，强转对象，以便fluent方式构建对象
	 * @param appRegister
	 * @return
	 */
	@Override
	public NettyServiceRegister setAppRegister(ApplicationRegister appRegister) {
		return (NettyServiceRegister) super.setAppRegister(appRegister);
	}

	@Override
	protected void doRegisterServices(List<ServiceConfig> serviceList) throws Exception {
		if(nettyServer != null){
			//服务线程存在时加入，否则跳过
			nettyServer.addServices(serviceList);
		}
	}

	/**
	 * 设置服务列表
	 * @param serviceList
	 * @return
	 */
	@Override
	public NettyServiceRegister setServiceList(List<ServiceConfig> serviceList) {
		//额外处理
		return (NettyServiceRegister) super.setServiceList(serviceList);
	}
}

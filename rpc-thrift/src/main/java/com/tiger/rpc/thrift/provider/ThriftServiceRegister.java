package com.tiger.rpc.thrift.provider;

import com.alibaba.fastjson.JSON;
import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.common.enums.ProtocolTypeEnum;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.common.register.ServiceRegister;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TMultiplexedProcessor;

import java.io.IOException;
import java.util.List;

/**
 * @ClassName: ThriftServiceRegister.java
 *
 * @Description: 注册服务提供者<br>
 * 				使用{@link TMultiplexedProcessor}发布多个服务<br>
 * 				格式:<br>
 * 				|rpc<br>
 * 				|- com.tiger.chaos.UserService_1.1.1<br>
 * 				|-- 192.168.1.21:8089:3<br>
 * 				|-- 192.168.1.22:8089:2
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/17
 */
@Slf4j
public class ThriftServiceRegister extends ServiceRegister {

	/**
	 * 服务端线程，静态常量，只保存一份
	 */
	private static ThriftServerThread serverThread;

	/**
	 * selector线程数
	 */
	private int selectorThreads;

	/**
	 * worker线程数
	 */
	private int workerThreads;

	public ThriftServiceRegister(ApplicationRegister appRegister, List<ServiceConfig> serviceList, int selectorThreads, int workerThreads, int serverPort) {
		super(appRegister, serviceList, serverPort);
		this.selectorThreads = selectorThreads;
		this.workerThreads = workerThreads;
	}

	@Override
	public ProtocolTypeEnum getProtocolType() {
		//响应thrift协议
		return ProtocolTypeEnum.THRIFT;
	}

	/**
	 * 构建函数之后，注册thrift服务
	 * @throws Exception
	 */
	@Override
	public void doRegister() throws Exception {
		//注册具体的应用，启动thrift线程
		if(this.isProtocolServiceServing()){
			return;
		}
		if(serverThread == null){
			//线程不存在时，直接创建线程
			serverThread = new ThriftServerThread(this.getClass().getSimpleName() + "_ThriftServerThread",
					super.getServiceBeanMap(), super.getServerPort(), selectorThreads, workerThreads);
			log.debug("Thrift serverThread[{}] created. ", serverThread.getName());
		} else if (!serverThread.isAlive()){
			//线程不存活时，先关闭，在重建
			try {
				//关闭线程，回收线程资源，并休眠200毫秒
				serverThread.stopServer();
				Thread.sleep(200);
			} catch (Exception e) {
				log.warn("Stop thrift server[{}] error", JSON.toJSONString(serverThread.getServer()));
			}
			//创建线程
			serverThread = new ThriftServerThread(this.getClass().getSimpleName() + "_ThriftServerThread",
					super.getServiceBeanMap(), super.getServerPort(), selectorThreads, workerThreads);
			log.debug("Thrift serverThread[{}] created. ", serverThread.getName());
		} else if (!serverThread.isServing()){
			//存活 & 不提供服务时，直接启动服务
			log.debug("Server thread exists, alive but not serving, and will be started. ");
		}
		//启动线程
		serverThread.start();
		log.debug("Thrift serverThread[{}] started", serverThread.getName());
	}

	@Override
	public boolean isProtocolServiceServing() {
		if(serverThread != null && serverThread.isAlive() && serverThread.isServing()){
			//线程存在 & 存活 & 服务，打印日志，返回服务正常运行
			log.debug("Thrift serverThread[{}] is serving, state[{}]", serverThread.getName(), serverThread.getState());
			return true;
		}
		log.debug("Service error, server thread[{}] . ", JSON.toJSONString(serverThread));
		return false;
	}

	/**
	 * jdk对象销毁时，注销thrift服务
	 * @throws Exception
	 */
	@Override
	public void doUnRegister() throws Exception {
		//关闭thrift服务线程
		log.debug("Start to doUnRegister, and will stop thrift server thread");
		if (serverThread != null) {
			serverThread.stopServer();
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

	public int getSelectorThreads() {
		return selectorThreads;
	}

	public ThriftServiceRegister setSelectorThreads(int selectorThreads) {
		this.selectorThreads = selectorThreads;
		return this;
	}

	public int getWorkerThreads() {
		return workerThreads;
	}

	public ThriftServiceRegister setWorkerThreads(int workerThreads) {
		this.workerThreads = workerThreads;
		return this;
	}

	/**
	 * 覆盖方法，强转对象，以便fluent方式构建对象
	 * @param appRegister
	 * @return
	 */
	@Override
	public ThriftServiceRegister setAppRegister(ApplicationRegister appRegister) {
		return (ThriftServiceRegister) super.setAppRegister(appRegister);
	}

	@Override
	protected void doRegisterServices(List<ServiceConfig> serviceList) throws Exception {
		if(serverThread != null){
			//服务线程存在时加入，否则跳过
			serverThread.addServices(serviceList);
		}

	}

	/**
	 * 设置服务列表
	 * @param serviceList
	 * @return
	 */
	@Override
	public ThriftServiceRegister setServiceList(List<ServiceConfig> serviceList) {
		//额外处理
		return (ThriftServiceRegister) super.setServiceList(serviceList);
	}
}

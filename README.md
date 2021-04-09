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
  >>rpc-thrift: 基于thrift协议框架，定制化rpc功能。   
  >>rpc-netty: 基于netty通信框架 & protostuff序列化工具，实现方法同步调用功能。   
  >>rpc-application-spring: rpc应用注册spring化。   
  >>rpc-thrift-spring: thrift协议的rpc spring功能。   
  >>rpc-netty-spring: netty通信的rpc spring功能。   
  
## 三、主要功能  
  >1.该框架基于zookeeper注册中心，引用端可以自动发现zookeeper上的服务提供者。   
  >
  >2.目前支持thrift & netty两种远程调用方式，并支持对服务的重试。   
  >
  >3.对thrift & netty等服务进行检测。对于异常情况，可以自动拉起。    
  >
  >4.通过外部实现SyncMachineService接口，能达到对机器和服务的信息同步，准实时检测机器和服务。   
  >
  >5.通过外部实现NoticeService接口，可以在服务出现异常时通知系统管理员。   
  >
  >6.zk上服务提供者监控task，将会监控服务的提供着，并对服务提供者小于指定阈值时，进行告警。
  
## 四、整体架构 & 主要功能图解     
  >![RPC架构V1](https://user-images.githubusercontent.com/19148139/114145569-2063b600-9949-11eb-9cdc-9da25519e110.png)

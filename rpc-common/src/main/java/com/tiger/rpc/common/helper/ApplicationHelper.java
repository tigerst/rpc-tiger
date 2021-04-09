package com.tiger.rpc.common.helper;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tiger.rpc.common.config.ServiceConfig;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.tiger.rpc.common.utils.BeanTransformUtil;
import com.tiger.rpc.common.utils.Constants;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.curator.framework.CuratorFramework;

import java.util.List;
import java.util.Map;

/**
 * @ClassName: ServiceHelper.java
 *
 * @Description: 应用服务工具：查询可用服务、提供者、消费者
 *
 * @Author: Tiger
 *
 * @Date: 2019/4/30
 */
public class ApplicationHelper {

    private String appPath;

    private CuratorFramework zkClient;

    public ApplicationHelper(ApplicationRegister appRegister){
        Preconditions.checkArgument(appRegister != null, "appRegister can not be null");
        Preconditions.checkArgument(appRegister.isRegistered(), "appRegister must be registered");
        //基路径
        this.appPath = appRegister.getAppPath();
        //获取应用
        this.zkClient = appRegister.getZkClient();
    }

    /**
     * 获取应用的所有协议
     * @return
     */
    public List<String> getApplicationProtocols() throws Exception {
        return zkClient.getChildren().forPath(appPath);
    }

    /**
     * 获取在使用的所有服务
     * @return
     * @throws Exception
     */
    public List<String> getServiceNames() throws Exception {
        List<String> serviceNames = Lists.newArrayList();
        //首先查询出所有协议
        List<String> protocols = zkClient.getChildren().forPath(appPath);
        /**
         * 遍历协议，获取每个协议的所有服务
         */
        for (String protocol : protocols) {
            serviceNames.addAll(zkClient.getChildren().forPath(appPath + Constants.PATH_SEPARATOR + protocol));
        }
        return serviceNames;
    }

    /**
     * zk上的所有服务
     *  1.获取协议
     *  2.获取每个协议的所有服务
     * @return
     * @throws Exception
     */
    public Map<String, List<String>> getServiceNamesGroupByProtocol() throws Exception {
        Map<String, List<String>> protocolserviceNamesMap = Maps.newHashMap();
        //首先查询出所有协议
        List<String> protocols = zkClient.getChildren().forPath(appPath);
        /**
         * 遍历协议，获取每个协议的所有服务，并组成key-value形式
         */
        for (String protocol : protocols) {
            protocolserviceNamesMap.put(protocol, zkClient.getChildren().forPath(appPath + Constants.PATH_SEPARATOR + protocol));
        }
        return protocolserviceNamesMap;
    }

    /**
     * 查询所有任务服务信息
     *  1.获取协议
     *  2.获取协议下的服务
     *  3.获取服务下的服务提供者和消费者
     *  4.服务提供者数据
     *  5.组装所有数据信息，并返回
     * @return
     * @throws Exception
     */
    public JSONObject getAllInfoFromZk() throws Exception {
        JSONObject jsonObject = new JSONObject();

        List<String> protocols = zkClient.getChildren().forPath(appPath);
        /**
         * 遍历协议，获取每个协议的所有服务，并组成key-value形式
         */
        List<String> protocolServices;
        List<String> providers;
        List<String> consumers = Lists.newArrayList();
        //所有服务列表
        List<ServiceConfig> configList = Lists.newArrayList();
        ServiceConfig config;
        Map serviceInfo;
        List<Map> serviceInfoList = Lists.newArrayList();
        List<String> elements;
        for (String protocol : protocols) {
            protocolServices = zkClient.getChildren().forPath(appPath + Constants.PATH_SEPARATOR + protocol);
            for (String service : protocolServices) {
                serviceInfo = Maps.newHashMap();
                elements = zkClient.getChildren().forPath(appPath + Constants.PATH_SEPARATOR + protocol
                        + Constants.PATH_SEPARATOR + service);
                for (String element : elements) {
                    if("consumers".equalsIgnoreCase(element)){
                        consumers = zkClient.getChildren().forPath(appPath + Constants.PATH_SEPARATOR + protocol
                                + Constants.PATH_SEPARATOR + service + Constants.PATH_SEPARATOR + element);
                    } else if("providers".equalsIgnoreCase(element)){
                        providers = zkClient.getChildren().forPath(appPath + Constants.PATH_SEPARATOR + protocol
                                + Constants.PATH_SEPARATOR + service + Constants.PATH_SEPARATOR + element);
                        for (String provider : providers) {
                            //转换成对象配置
                            config = JSON.parseObject(zkClient.getData().forPath(appPath + Constants.PATH_SEPARATOR + protocol
                                            + Constants.PATH_SEPARATOR + service + Constants.PATH_SEPARATOR + element + Constants.PATH_SEPARATOR + provider),
                                    ServiceConfig.class);
                            configList.add(config);
                        }
                    }
                }
                //收集providers和consumers
                serviceInfo.put("providers", BeanTransformUtil.transformToServerPacketGroupByHost(configList));
                serviceInfo.put("consumers", consumers);
                serviceInfoList.add(serviceInfo);
            }
            jsonObject.put(protocol, serviceInfoList);
        }
        return jsonObject;
    }

}

package com.tiger.rpc.common.helper;

import com.tiger.rpc.common.config.ReferenceConfig;
import com.tiger.rpc.common.enums.ServiceCodeEnum;
import com.tiger.rpc.common.exception.ServiceException;
import com.tiger.rpc.common.register.ReferenceRegister;
import com.tiger.rpc.common.utils.Constants;
import com.tiger.rpc.common.utils.ProviderParser;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.curator.framework.CuratorFramework;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: ReferenceHelper.java
 *
 * @Description: 应用服务：对比服务提供者、选择机器
 *
 * @Author: Tiger
 *
 * @Date: 2019/4/30
 */
@Getter
@Slf4j
public class ReferenceHelper {

    private ReferenceRegister register;

    private CuratorFramework zkClient;

    public ReferenceHelper(ReferenceRegister register){
        this.register = register;
        this.zkClient = register.getAppRegister().getZkClient();
    }

    /**
     * 对比zk与local的provider
     */
    public Map<String, List<String>> differProviders() throws Exception {
        Map<String, List<String>> differMap = Maps.newHashMap();
        if(CollectionUtils.isEmpty(register.getReferenceBeanMap().values())){
            return differMap;
        }
        String providersPath;
        List<String> providers;
        String serviceName;
        List<String> singleDiffers;
        for (ReferenceConfig referenceConfig : register.getReferenceBeanMap().values()) {
            providersPath = register.assembleProvidersPath(referenceConfig);
            //查询provider目录
            providers = zkClient.getChildren().forPath(providersPath);
            serviceName = register.getServiceNameByConf(referenceConfig);
            singleDiffers = (List<String>)CollectionUtils.subtract(ProviderParser.parseProviders(providers),
                    register.getServiceProvidersMap().get(serviceName));
            if(CollectionUtils.isNotEmpty(singleDiffers)){
                //有差别时
                differMap.put(serviceName, singleDiffers);
            }
        }
        return differMap;
    }

    /**
     * 获取provider地址，传入小集群地址时，从小集群选择，否则从大集群选择
     * @param serviceName   服务名
     * @param version   版本号
     * @param uris   小集群路径
     * @return  provider
     */
    public String getAddress(String serviceName, String version, List<String> uris) throws ServiceException {
        //zk上的服务名（服务加版本号）
        String key = null;
        try {
            String serviceWithVersion = serviceName + Constants.APPLICATION_VERSION_SEPARATOR + version;
            //查询当前可用的providers
            List<String> addressList = register.getServiceProvidersMap().get(serviceWithVersion);
            if (CollectionUtils.isEmpty(addressList)) {
                throw new ServiceException(ServiceCodeEnum.SERVICE_NO_AVAILABLE_PROVIDERS.getCode(),
                        String.format(ServiceCodeEnum.SERVICE_NO_AVAILABLE_PROVIDERS.getValue(), serviceName));
            }

            /**
             * 策略选择
             */
            if(CollectionUtils.isEmpty(uris)){
                //未指定小集群时，根据策略选择从大集群中选择
                log.debug("uri is not set, will do choices by strategy");
                key = register.getProviderStrategy().getProvider(addressList);
            } else {
                //指定小集群时，根据策略从小集群中选择
                String uri = register.getProviderStrategy().getProvider(uris);
                log.debug("uri[{}] is chosen, will do customized way", uri);
                key = analyseCustomizedUri(uri, addressList);
                if(StringUtils.isBlank(key)){
                    //在没有发现机器时，抛出异常
                    throw new ServiceException(ServiceCodeEnum.PROVIDER_NOT_FOUND.getCode(),
                            String.format(ServiceCodeEnum.PROVIDER_NOT_FOUND.getValue(), serviceName, uri));
                }
            }
        } catch (ServiceException e) {
            log.error("Get address error", e);
            throw e;
        }
        log.debug("The selected address: {}", key);
        return key;
    }

    /**
     * 分析定制化uri
     * @param uri   定制化路径
     * @param addressList 地址列表
     * @return  处理后的定制化路径
     */
    private String analyseCustomizedUri(String uri, List<String> addressList) {
        String[] uriElements = uri.split(Constants.PROTOCOL_HOST_SEPARATOR);
        if(uriElements == null || uriElements.length != 2){
            throw new ServiceException(ServiceCodeEnum.PROVIDER_URI_NOT_ILLEGAL.getCode(),
                    String.format(ServiceCodeEnum.PROVIDER_URI_NOT_ILLEGAL.getValue(), uri));
        }
        String address = uriElements[1];
        String[] elements = address.split(Constants.HOST_PORT_SEPARATOR);
        if(elements == null || elements.length != 2){
            throw new ServiceException(ServiceCodeEnum.PROVIDER_URI_NOT_ILLEGAL.getCode(),
                    String.format(ServiceCodeEnum.PROVIDER_URI_NOT_ILLEGAL.getValue(), uri));
        }
        String key = null;
        //host不校验是否为ip，兼容网络主机名
        String host = elements[0];
        String portStr = elements[1];
        //复制到数据，防止zk连接改动，产生连接抖动
        List<String> hostPortList = Lists.newArrayList(addressList);
        if(Constants.NULL_STR.equalsIgnoreCase(portStr)){
            //port不存在时，匹配ip:[1-65535]正则表达式
            String regex = Constants.LINE_START_REGEX + host + Constants.HOST_PORT_SEPARATOR + Constants.PORT_VALUE_REGEX;
            for (String hostPort : hostPortList) {
                if(hostPort.matches(regex)){
                    //匹配到路径直接返回
                    key = hostPort;
                    break;
                }
            }
        } else {
            //port存在时
            int port = NumberUtils.toInt(portStr, 0);
            if(port <= 0 || port > Constants.PORT_MAX_VALUE){
                //端口号范围有问题(0, 65535]
                throw new ServiceException(ServiceCodeEnum.PROVIDER_URI_NOT_ILLEGAL.getCode(),
                        String.format(ServiceCodeEnum.PROVIDER_URI_NOT_ILLEGAL.getValue(), uri));
            }
            key = host + Constants.HOST_PORT_SEPARATOR + port;
            if(!hostPortList.contains(key)){
                //校验，不存在时异常
                throw new ServiceException(ServiceCodeEnum.PROVIDER_URI_NOT_ILLEGAL.getCode(),
                        String.format(ServiceCodeEnum.PROVIDER_URI_NOT_ILLEGAL.getValue(), uri));
            }
        }
        return key;
    }

    /**
     * 校验方法级别的provider
     * @param provider   provider
     * @param method    方法
     * @param args  参数
     * @return  返回校验结果
     */
    public void checkAddress(String provider, Method method, Object[] args) {
        register.getProviderStrategy().checkProvider(provider, method, args);
    }

}

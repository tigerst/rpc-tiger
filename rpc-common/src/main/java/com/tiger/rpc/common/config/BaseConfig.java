package com.tiger.rpc.common.config;

import com.tiger.rpc.common.utils.NetworkUtils;
import com.tiger.rpc.common.utils.ProcessUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.UUID;

/**
 * @ClassName: BaseConfig.java
 *
 * @Description: 基础配置，供service和reference引用
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/26
 */
public class BaseConfig<E> implements Serializable {

    /**
     * 编号，默认uuid
     */
    private String id = UUID.randomUUID().toString();

    /**
     * 是否使用host标记
     */
    private transient boolean useHostName = false;

    /**
     * jvm程编号
     */
    private int pid = ProcessUtils.pid();

    /**
     * 当前机器ip
     */
    private String ip = NetworkUtils.ip();

    /**
     * 当前机器主机名
     */
    private String hostName = NetworkUtils.host();

    /**
     * 协议类型：protocol(thrift/netty)
     */
    private String protocol;

    /**
     * 服务版本号
     */
    private String version = "1.0.0";

    /**
     * 接口类型
     */
    private String interfaceName;

    /**
     * 接口类
     */
    private Class<?> interfaceClass;

    public String getId() {
        return id;
    }

    public <T extends BaseConfig> T setId(String id) {
        this.id = id;
        return (T) this;
    }

    public int getPid() {
        return pid;
    }

    public boolean isUseHostName() {
        return useHostName;
    }

    public <T extends BaseConfig> T setUseHostName(boolean useHostName) {
        this.useHostName = useHostName;
        return (T) this;
    }

    public String getHost() {
        if(this.useHostName){
            return hostName;
        }
        return ip;
    }

    public String getIp() {
        return ip;
    }

    public String getHostName() {
        return hostName;
    }

    public String getProtocol() {
        return protocol;
    }

    public <T extends BaseConfig> T setProtocol(String protocol) {
        this.protocol = protocol;
        return (T) this;
    }

    public String getVersion() {
        return version;
    }

    public <T extends BaseConfig> T setVersion(String version) {
        if(StringUtils.isBlank(version)){
            this.version = "1.0.0";
        } else {
            this.version = version;
        }
        return (T) this;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    /**
     * 私有方法，不开放给外部，设置接口时，连带设置
     * @param interfaceName
     * @param <T>
     * @return
     */
    private <T extends BaseConfig> T setInterfaceName(String interfaceName) {
        setId(interfaceName);
        this.interfaceName = interfaceName;
        return (T) this;
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        return interfaceClass;
    }

    public <T extends BaseConfig> T  setInterfaceClass(Class<?> interfaceClass) {
        if (interfaceClass != null && ! interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
        //设则接口名称
        setInterfaceName(interfaceClass.getName());
        return (T) this;
    }
}

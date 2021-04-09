package com.tiger.rpc.common.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @ClassName: ApplicationConfig.java
 *
 * @Description: 应用配置
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/19
 */
@Slf4j
public class ApplicationConfig {

    /**
     * 应用名称：默认机器ip
     */
    private String name;

    /**
     * 组织名(BU或部门)
     */
    private String group;

    /**
     * 应用负责人
     * 多个负责人时，使用符号|隔开
     */
    private String owner;

    /**
     * 环境，如：dev/test/run
     */
    private String env;

    /**
     * 所加入的集群，默认bdp
     */
    private String cluster = "bdp";

    /**
     * 版本，默认为1.0.0
     */
    private String version = "1.0.0";

    public String getName() {
        return name;
    }

    public ApplicationConfig setName(String name) {
        this.name = name;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public ApplicationConfig setGroup(String group) {
        this.group = group;
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public ApplicationConfig setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public String getEnv() {
        return env;
    }

    public ApplicationConfig setEnv(String env) {
        this.env = env;
        return this;
    }

    public String getCluster() {
        return cluster;
    }

    public ApplicationConfig setCluster(String cluster) {
        if(StringUtils.isBlank(cluster)){
            this.cluster = "bdp";
        } else {
            this.cluster = cluster;
        }
        return this;
    }

    public String getVersion() {
        return version;
    }

    public ApplicationConfig setVersion(String version) {
        if(StringUtils.isBlank(version)){
            this.version = "1.0.0";
        } else {
            this.version = version;
        }
        return this;
    }
}

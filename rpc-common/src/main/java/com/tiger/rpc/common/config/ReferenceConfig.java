package com.tiger.rpc.common.config;

/**
 * @ClassName: ReferenceConfig.java
 *
 * @Description: 引用配置
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/19
 */
public class ReferenceConfig<T> extends BaseConfig<T> {

    /**
     * 接口代理器
     * 不序列话
     */
    private transient T proxy;

    /**
     * 重试次数，默认2次，不算第一次执行
     * 为0时，只执行一次，不重试
     */
    private int retry = 2;

    public T getProxy() {
        return proxy;
    }

    public ReferenceConfig<T> setProxy(T proxy) {
        this.proxy = proxy;
        return this;
    }

    public int getRetry() {
        return retry;
    }

    public ReferenceConfig<T> setRetry(int retry) {
        if(retry < 0){
            this.retry = 2;
        } else {
            this.retry = retry;
        }
        return this;
    }
}

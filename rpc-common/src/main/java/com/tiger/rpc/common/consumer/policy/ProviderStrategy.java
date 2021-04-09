package com.tiger.rpc.common.consumer.policy;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @ClassName: SelectPolicy.java
 *
 * @Description: 服务选择策略
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/18
 */
public interface ProviderStrategy<T> {

    /**
     * 无定制化的服务发现机制
     * @param tList
     * @return
     */
    public T getProvider(List<T> tList);

    /**
     * 校验方法级别的provider
     * @param provider  当前选中的provider
     * @param method    方法
     * @param args  参数
     * @return
     */
    public void checkProvider(T provider, Method method, Object[] args);

}

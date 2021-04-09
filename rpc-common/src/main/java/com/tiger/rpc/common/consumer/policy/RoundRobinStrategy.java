package com.tiger.rpc.common.consumer.policy;

import com.tiger.rpc.common.enums.ServiceCodeEnum;
import com.tiger.rpc.common.exception.ServiceException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @ClassName: RoundRobinSelectPolicy.java
 *
 * @Description: RoundRobin选择策略
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/20
 */
public class RoundRobinStrategy<T> implements ProviderStrategy<T> {

    private AtomicLong nextIndex = new AtomicLong(0);

    @Override
    public T getProvider(List<T> tList) {
        if(CollectionUtils.isEmpty(tList)){
            return null;
        }
        //取余策略
        if(nextIndex.longValue() >= Long.MAX_VALUE){
            //超过long的最大值时，从头开始
            nextIndex = new AtomicLong(0);
        }
        return tList.get((int) nextIndex.getAndIncrement() % tList.size());
    }

    @Override
    public void checkProvider(T provider, Method method, Object[] args) {
        // TODO: 2019/7/22 默认round robin不做处理
        if(provider == null){
            throw new ServiceException(ServiceCodeEnum.SERVICE_NO_AVAILABLE_PROVIDERS.getCode(), "No provider for this consumer");
        }
        if(provider instanceof String && StringUtils.isBlank((String)provider)){
            //字符串时
            throw new ServiceException(ServiceCodeEnum.SERVICE_NO_AVAILABLE_PROVIDERS.getCode(), "No provider for this consumer");
        }
    }
}

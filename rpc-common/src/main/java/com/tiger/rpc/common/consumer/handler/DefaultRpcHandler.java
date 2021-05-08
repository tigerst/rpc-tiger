package com.tiger.rpc.common.consumer.handler;

import com.alibaba.fastjson.JSON;
import com.tiger.rpc.common.enums.ServiceCodeEnum;
import com.tiger.rpc.common.exception.ServiceException;
import com.tiger.rpc.common.helper.ReferenceHelper;
import com.tiger.rpc.common.register.ReferenceRegister;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @ClassName: DefaultRpcHandler.java
 *
 * @Description: rpc通用服务代理
 *
 * @Author: Tiger
 *
 * @Date: 2019/7/22
 */
@Data
@Slf4j
public abstract class DefaultRpcHandler<T> implements InvocationHandler, Closeable {

    /**
     * socket连接池
     */
    private GenericKeyedObjectPool<String, T> pool;

    /**
     * 引入发现服务工具
     */
    private ReferenceHelper helper;

    /**
     * 服务版本号
     */
    private String serviceVersion;

    /**
     * 重试次数
     */
    private int retry;

    /**
     * 传入机器uri，默认为null
     * protocol(thrift/netty)://ip:port
     * protocol(thrift/netty)://ip:null
     */
    private String uri = null;

    public DefaultRpcHandler(){

    }

    public DefaultRpcHandler(GenericKeyedObjectPool<String, T> pool){
        if (pool == null) {
            throw new ServiceException(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getCode(),
                    String.format(ServiceCodeEnum.MISS_REQUIRED_PARAMETER.getValue(), "pool"));
        }
        this.pool = pool;
    }

    public DefaultRpcHandler(ReferenceRegister discovery){
        this.helper = new ReferenceHelper(discovery);
    }
    
    @Override
    public void close() throws IOException {
        /**
         * 置空参数，加速回收
         */
        this.pool = null;
        this.helper = null;
        this.uri = null;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result = null;
        try {
            //执行逻辑
            result = process(method, args);
        } catch (Throwable e) {
            throw e;
        }
        return result;
    }

    /**
     * 循环调用，实现重试重试机制
     * @param method
     * @param args
     * @return
     */
    protected Object process(Method method, Object[] args) throws Throwable {
        boolean successFlag = false;
        //protocol(thrift/netty) server : port : weight
        String key = null;
        T tSocket = null;
        Object result = null;
        Object client = null;
        int counter = -1;
        while (counter < retry && !successFlag) {
            //执行次数计数器递增
            counter++;
            try {
                key = getKey(method, args);
                //连接池爆满后，borrowObject将会跑异常java.util.NoSuchElementException: Timeout waiting for idle object
                tSocket = pool.borrowObject(key);
                //生成具体对象
                client = getClient(tSocket, method);
                //调用方法，接口方法，远程获取结果
                result = callRemoteMethod(client, method, args);
                //设置处理成功
                successFlag = true;
                //记录成功日志
                doSuccessLog(method.getDeclaringClass().getName(), method.getName(), args, counter);
            } catch (Throwable e) {
                //使用异常超类捕获，防止因反射异常引起未识别异常java.lang.reflect.UndeclaredThrowableException
                //处理异常
                Throwable tw = processException(e, counter, key, tSocket);
                if(tw != null){
                    //终止时，打印错误日志
                    doFailureLog(method.getDeclaringClass().getName(), method.getName(), args, counter, e);
                    //抛出异常
                    throw tw;
                } else {
                    //非终止时，debug
                    if(counter == 0){
                        log.debug("Method[{}] params[{}] execute error[{}]", this.getClass() +"." + method.getName(), JSON.toJSONString(args), e.getMessage());
                    } else {
                        //重试
                        log.debug("Method[{}] params[{}] retry [{}] times error[{}]", this.getClass() +"." + method.getName(), JSON.toJSONString(args), counter, e.getMessage());
                    }
                }
            } finally {
                if(tSocket != null){
                    //最终处理
                    processFinally(key, client, tSocket);
                    //退出
                    pool.returnObject(key, tSocket);
                }
            }
        }
        return result;
    }

    /**
     * 远程调用，此处可以让子类覆盖，实现定制化处理
     * @param client    client对象
     * @param method    方法对象
     * @param args      方法参数
     * @param client
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    protected Object callRemoteMethod(Object client, Method method, Object[] args) throws Throwable{
        return method.invoke(client, args);
    }

    /**
     * 最终处理
     * @param key
     * @param client
     * @param tsocket
     */
    protected abstract void processFinally(String key, Object client, T tsocket);

    /**
     * 异常处理
     * socket异常时，需要校验socket
     * 参数异常时，直接推出，不需要重试
     * @param exception 异常
     * @param counter   计数器
     * @param key   机器
     * @param tsocket   连接
     * @return
     * @throws Throwable
     */
    protected abstract Throwable processException(Throwable exception, int counter, String key, T tsocket);

    /**
     * 异常日志
     * @param methodName
     * @param args
     * @param counter
     * @param e
     */
    private void doFailureLog(String clazz, String methodName, Object[] args, int counter, Throwable e){
        this.doLog(clazz, methodName, args, false, counter, e);
    }

    /**
     * 成功日志
     * @param methodName    调用方法
     * @param args
     * @param counter
     */
    private void doSuccessLog(String clazz, String methodName, Object[] args, int counter){
        this.doLog(clazz, methodName, args, true, counter, null);
    }

    /**
     * 记录日志
     * @param methodName    方法
     * @param successFlag   操作成功标记
     * @param counter   操作计数器
     */
    private void doLog(String clazz, String methodName, Object[] args, boolean successFlag, int counter, Throwable e) {
        if(successFlag){
            //成功日志记录，每次执行情况
            if(counter == 0){
                log.info("Method[{}] params[{}] execute successfully", clazz +"." + methodName, JSON.toJSONString(args));
            } else {
                //重试
                log.info("Method[{}] params[{}] retry [{}] times successfully", clazz +"." + methodName, JSON.toJSONString(args), counter);
            }
        } else {
            //异常日志记录，每次执行情况
            if(counter == 0){
                log.error("Method[{}] params[{}] execute error", clazz +"." + methodName, JSON.toJSONString(args), e);
            } else {
                //重试
                log.error("Method[{}] params[{}] retry [{}] times error", clazz +"." + methodName, JSON.toJSONString(args), counter, e);
            }
        }
    }

    /**
     * 获取具体对象
     * @param tSocket   连接/管道
     * @param method   方法
     * @return
     */
    protected abstract Object getClient(T tSocket, Method method) throws Exception;


    /**
     * 获取key：每次需要换着取，防止网络延迟及其宕机异常情况。其选择策略在discovery中设置的
     * @return
     */
    protected String getKey(Method method, Object[] args) throws ServiceException {
        String key;
        if (this.helper != null) {
            //引入发现服务工具情况
            Class<?> enClosedClazz = method.getDeclaringClass().getEnclosingClass();
            enClosedClazz = enClosedClazz == null? method.getDeclaringClass() : enClosedClazz;
            //1.获取地址
            key = this.helper.getAddress(enClosedClazz.getName(), serviceVersion, uri);
            //2.校验地址(控制到方法级别)
            helper.checkAddress(key, method, args);
        } else {
            //未引入发现服务工具情况
            //1.获取地址
            key = this.getAddress(uri);
            //2.校验地址(控制到方法级别)
            this.checkAddress(uri, method, args);
        }
        return key;
    }

    protected void checkAddress(String uri, Method method, Object[] args) {
        //不做处理，交给具体实现类处理
    }

    public String getAddress(String uri) {
        //默认方法直接返回，交给具体实现类处理
        return uri;
    }

}

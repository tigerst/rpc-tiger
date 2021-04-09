package com.tiger.rpc.common.exception;

/**
 * @ClassName: ServiceException.java
 *
 * @Description: 服务异常，继承RuntimeException，而非Exception
 *              继承Exception被定义成了检查型异常，会导致被包装成了UndeclaredThrowableException，覆盖异常信息
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/23
 */
public class ServiceException extends RuntimeException {
    private String code;

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String code, String message) {
        this(message);
        this.setCode(code);
    }

    public ServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.setCode(code);
    }

    public ServiceException(String code, Throwable cause) {
        super(cause);
        this.setCode(code);
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}

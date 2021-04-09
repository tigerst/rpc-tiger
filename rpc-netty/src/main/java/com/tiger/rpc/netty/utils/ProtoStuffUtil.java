package com.tiger.rpc.netty.utils;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import lombok.NoArgsConstructor;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName: ProtoStuffUtil.java
 *
 * @Description: protoStuff序列化工具，用于rpc数据包传输
 *               1.fastjson反序列化有问题（类型丢失、自定义异常序列化等），因此不使用此fastjson
 *               2.使用常用的rpc序列化工具protostuff
 *               3.如果此处使用fastjson，则对参数/结果的复杂对象需要手动转换
 *               4.protoStuff提升性能，减少空间占用
 *
 * @Author: Tiger
 *
 * @Date: 2021/3/30
 */
@NoArgsConstructor
public class ProtoStuffUtil {

    /**
     * 缓存class <---> schema 键值对
     */
    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();

    /**
     * 使用objenesis创建对象，绕过构造器
     */
    private static Objenesis objenesis = new ObjenesisStd(true);

    /**
     * 序列化（对象 -> 字节数组）
     * @param obj   对象
     * @param <T>   对象范型
     * @return
     */
    public static <T> byte[] serialize(T obj) {
        Class<T> cls = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(cls);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 反序列化（字节数组 -> 对象）
     * @param data  数据
     * @param clazz   类
     * @param <T>
     * @return
     */
    public static <T> T deserialize(byte[] data, Class<T> clazz) {
        try {
            T message = objenesis.newInstance(clazz);
            Schema<T> schema = getSchema(clazz);
            ProtostuffIOUtil.mergeFrom(data, message, schema);
            return message;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * 获取class的schema
     * @param clazz
     * @param <T>
     * @return
     */
    private static <T> Schema<T> getSchema(Class<T> clazz) {
        Schema<T> schema = (Schema<T>) cachedSchema.get(clazz);
        if (schema == null) {
            schema = RuntimeSchema.createFrom(clazz);
            cachedSchema.put(clazz, schema);
        }
        return schema;
    }
}

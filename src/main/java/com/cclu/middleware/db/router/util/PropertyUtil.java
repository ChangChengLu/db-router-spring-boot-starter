package com.cclu.middleware.db.router.util;

import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author ChangCheng Lu
 * @date 2023/11/11 16:28
 * @description 属性工具类
 * @copyright ChangChengLu
 */
public class PropertyUtil {

    private static int springBootVersion = 1;

    static {
        try {
            Class.forName("org.springframework.boot.bind.RelaxedPropertyResolver");
        } catch (ClassNotFoundException e) {
            springBootVersion = 2;
        }
    }

    /**
     * Spring Boot 1.x is compatible with Spring Boot 2.x by Using Java Reflect.
     * @param environment the environment context
     * @param prefix the prefix part of property key
     * @param targetClass the target class type of result
     * @param <T> refer to @param targetClass
     * @return T
     */
    @SuppressWarnings("unchecked")
    public static <T> T handle(final Environment environment, final String prefix, final Class<T> targetClass) {
        switch (springBootVersion) {
            case 1:
                return (T) v1(environment, prefix);
            default:
                return (T) v2(environment, prefix, targetClass);
        }
    }

    private static Object v1(final Environment environment, final String prefix) {
        try {
            Class<?> resolverClass = Class.forName("org.springframework.boot.bind.RelaxedPropertyResolver");
            Constructor<?> resolverConstructor = resolverClass.getDeclaredConstructor(PropertyResolver.class);
            Method getSubPropertiesMethod = resolverClass.getDeclaredMethod("getSubProperties", String.class);
            Object resolverObject = resolverConstructor.newInstance(environment);
            String prefixParam = prefix.endsWith(".") ? prefix : prefix + ".";
            return getSubPropertiesMethod.invoke(resolverObject, prefixParam);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
                | InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * 处理配置文件信息，将配置文件部分 prefix 前缀配置转换为 targetClass 类型对象并返回
     * @param environment profile抽象
     * @param prefix profile配置前缀
     * @param targetClass 目标返回类型
     * @return targetClass类型对象
     */
    private static Object v2(final Environment environment, final String prefix, final Class<?> targetClass) {
        try {
            Class<?> binderClass = Class.forName("org.springframework.boot.context.properties.bind.Binder");
            Method getMethod = binderClass.getDeclaredMethod("get", Environment.class);
            Method bindMethod = binderClass.getDeclaredMethod("bind", String.class, Class.class);
            // 获取 Binder 类型实例
            Object binderObject = getMethod.invoke(null, environment);
            // 如何 prefix 以 . 号结尾，则去除 . 号
            String prefixParam = prefix.endsWith(".") ? prefix.substring(0, prefix.length() - 1) : prefix;
            // 将 prefix 前缀的属性转换为 targetClass 类型实例，并绑定到 bindResultObject 内部 value 属性上
            Object bindResultObject = bindMethod.invoke(binderObject, prefixParam, targetClass);
            // 获取 BinderResult 属性的 get() 方法
            Method resultGetMethod = bindResultObject.getClass().getDeclaredMethod("get");
            // 使用 binderResult.get() 方法获取 binderResult 内部 value 属性值
            return resultGetMethod.invoke(bindResultObject);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException
                | IllegalAccessException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

}

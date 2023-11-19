package com.cclu.middleware.db.router;

import com.cclu.middleware.db.router.annotation.DBRouter;
import com.cclu.middleware.db.router.strategy.IDBRouterStrategy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author ChangCheng Lu
 * @date 2023/11/11 20:14
 * @description 数据路由切面，通过自定义注解的方式，拦截被切面的方法，进行数据库路由
 * @copyright ChangChengLu
 */
@Aspect
@Slf4j
@AllArgsConstructor
public class DBRouterJoinPoint {

    /**
     * 数据路由配置信息
     */
    private DBRouterConfig dbRouterConfig;

    /**
     * 数据路由策略
     */
    private IDBRouterStrategy dbRouterStrategy;

    @Pointcut("@annotation(com.cclu.middleware.db.router.annotation.DBRouter)")
    public void aopPoint() {
    }

    /**
     * 所有需要分库分表的操作，都需要使用自定义注解进行拦截，拦截后读取方法中的入参字段，根据字段进行路由操作。
     * 1. dbRouter.key() 确定根据哪个字段进行路由
     * 2. getAttrValue 根据数据库路由字段，从入参中读取出对应的值。比如路由 key 是 uId，那么就从入参对象 Obj 中获取到 uId 的值。
     * 3. dbRouterStrategy.doRouter(dbKeyAttr) 路由策略根据具体的路由值进行处理
     * 4. 路由处理完成比，就是放行。 jp.proceed();
     * 5. 最后 dbRouterStrategy 需要执行 clear 因为这里用到了 ThreadLocal 需要手动清空。关于 ThreadLocal 内存泄漏介绍 https://t.zsxq.com/027QF2fae
     */
    @Around("aopPoint() && @annotation(dbRouter)")
    public Object doRouter(ProceedingJoinPoint jp, DBRouter dbRouter) throws Throwable {
        String dbKey = dbRouter.key();
        if (StringUtils.isBlank(dbKey) && StringUtils.isBlank(dbRouterConfig.getRouterKey())) {
            throw new RuntimeException("annotation DBRouter key is null!");
        }
        dbKey = StringUtils.isNotBlank(dbKey) ? dbKey : dbRouterConfig.getRouterKey();
        // 路由属性
        String dbKeyAttr = getAttrValue(dbKey, jp.getArgs());
        // 路由策略
        dbRouterStrategy.doRouter(dbKeyAttr);
        // 返回结果
        try {
            return jp.proceed();
        } finally {
            dbRouterStrategy.clear();
        }
    }

    private Method getMethod(JoinPoint jp) throws NoSuchMethodException {
        Signature sig = jp.getSignature();
        MethodSignature methodSignature = (MethodSignature) sig;
        // jp.getTarget(): 返回代理对象
        return jp.getTarget().getClass().getMethod(methodSignature.getName(), methodSignature.getParameterTypes());
    }

    /**
     * 计算路由属性
     * @param attr 分库分表字段 dbKey
     * @param args 切点方法参数数组
     * @return 计算后的路由属性
     */
    public String getAttrValue(String attr, Object[] args) {
        if (1 == args.length) {
            Object arg = args[0];
            if (arg instanceof String) {
                return arg.toString();
            }
        }

        String filedValue = null;
        for (Object arg : args) {
            try {
                if (StringUtils.isNotBlank(filedValue)) {
                    break;
                }
                // filedValue = BeanUtils.getProperty(arg, attr);
                // fix: 使用lombok时，uId这种字段的get方法与idea生成的get方法不同，会导致获取不到属性值，改成反射获取解决
                filedValue = String.valueOf(getValueByName(arg, attr));
            } catch (Exception e) {
                log.error("获取路由属性值失败 attr：{}", attr, e);
            }
        }
        return filedValue;
    }

    /**
     * 获取对象的特定属性值
     *
     * @author tang
     * @param item 对象
     * @param name 属性名
     * @return 属性值
     */
    private Object getValueByName(Object item, String name) {
        try {
            Field field = getFieldByName(item, name);
            if (field == null) {
                return null;
            }
            // 将字段访问权限设置为公有可访问
            field.setAccessible(true);
            // 获取该字段的值，并将其赋值给对象 o
            Object o = field.get(item);
            // 重新将字段的访问权限设置为私有
            field.setAccessible(false);
            return o;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 根据名称获取字段，该方法同时兼顾继承类获取父类的属性
     *
     * @author tang
     * @param item 对象
     * @param name 属性名
     * @return 该属性对应的字段
     */
    private Field getFieldByName(Object item, String name) {
        try {
            Field field;
            try {
                // 获取对象 item 中名称为 name 的字段
                field = item.getClass().getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                // 子类没有名称为 name 的字段，从父类的中搜索名称为 name 的字段
                field = item.getClass().getSuperclass().getDeclaredField(name);
            }
            return field;
        } catch (NoSuchFieldException e) {
            // 子类和父类均没有名称为 name 的字段，返回 NULL
            return null;
        }
    }

}

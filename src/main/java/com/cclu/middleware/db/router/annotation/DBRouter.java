package com.cclu.middleware.db.router.annotation;

import java.lang.annotation.*;

/**
 * @author ChangCheng Lu
 * @date 2023/11/11 15:46
 * @description 数据库路由注解
 * @copyright ChangChengLu
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface DBRouter {

    /**
     * @return 分库分表字段
     */
    String key() default "";

}

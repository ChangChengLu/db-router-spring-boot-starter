package com.cclu.middleware.db.router.annotation;

import java.lang.annotation.*;

/**
 * @author ChangCheng Lu
 * @date 2023/11/16 11:36
 * @description 路由策略，分表标记
 * @copyright ChangChengLu
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DBRouterStrategy {

    /**
     *
     * @return 分表标记
     */
    boolean splitTable() default false;

}

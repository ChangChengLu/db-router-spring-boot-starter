package com.cclu.middleware.db.router.strategy;

/**
 * @author ChangCheng Lu
 * @date 2023/11/11 20:16
 * @description 路由策略
 * @copyright ChangChengLu
 */
public interface IDBRouterStrategy {

    /**
     * 路由计算
     * @param dbKeyAttr 路由字段
     */
    void doRouter(String dbKeyAttr);

    /**
     * 手动设置分库路由
     * @param dbIdx 路由库，需要在配置范围内
     */
    void setDBKey(int dbIdx);

    /**
     * 手动设置分表路由
     *
     * @param tbIdx 路由表，需要在配置范围内
     */
    void setTBKey(int tbIdx);

    /**
     * 获取分库数
     *
     * @return 数量
     */
    int dbCount();

    /**
     * 获取分表数
     *
     * @return 数量
     */
    int tbCount();

    /**
     * 清除路由
     */
    void clear();

}

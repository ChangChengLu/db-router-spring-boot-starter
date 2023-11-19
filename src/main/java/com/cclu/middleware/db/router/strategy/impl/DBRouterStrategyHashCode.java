package com.cclu.middleware.db.router.strategy.impl;

import com.cclu.middleware.db.router.DBContextHolder;
import com.cclu.middleware.db.router.DBRouterConfig;
import com.cclu.middleware.db.router.strategy.IDBRouterStrategy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ChangCheng Lu
 * @date 2023/11/11 20:19
 * @description 哈希路由
 * @copyright ChangChengLu
 */
@Slf4j
@AllArgsConstructor
public class DBRouterStrategyHashCode implements IDBRouterStrategy {

    private DBRouterConfig dbRouterConfig;

    @Override
    public void doRouter(String dbKeyAttr) {
        int size = dbRouterConfig.getDbCount() * dbRouterConfig.getTbCount();

        // 扰动函数；在 JDK 的 HashMap 中，对于一个元素的存放，需要进行哈希散列。而为了让散列更加均匀，所以添加了扰动函数。
        int idx = (size - 1) & (dbKeyAttr.hashCode() ^ (dbKeyAttr.hashCode() >>> 16));

        // 库表索引；相当于是把一个长条的桶，切割成段，对应分库分表中的库编号和表编号
        // 公式目的；8个位置，计算出来的是位置在5 那么你怎么知道5是在2库1表。
        int dbIdx = idx / dbRouterConfig.getTbCount() + 1;
        int tbIdx = idx - dbRouterConfig.getTbCount() * (dbIdx - 1);

        // 设置到 ThreadLocal
        setDBKey(dbIdx);
        setTBKey(tbIdx);
        log.debug("数据库路由 dbIdx：{} tbIdx：{}",  dbIdx, tbIdx);
    }

    @Override
    public void setDBKey(int dbIdx) {
        DBContextHolder.setDBKey(String.format("%02d", dbIdx));
    }

    @Override
    public void setTBKey(int tbIdx) {
        DBContextHolder.setTBKey(String.format("%03d", tbIdx));
    }

    @Override
    public int dbCount() {
        return dbRouterConfig.getDbCount();
    }

    @Override
    public int tbCount() {
        return dbRouterConfig.getTbCount();
    }

    @Override
    public void clear() {
        DBContextHolder.clearDBKey();
        DBContextHolder.clearTBKey();
    }

}

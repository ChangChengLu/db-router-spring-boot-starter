package com.cclu.middleware.db.router.dynamic;

import com.cclu.middleware.db.router.DBContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * @author ChangCheng Lu
 * @date 2023/11/11 16:13
 * @description 动态数据源获取，每当切换数据源，都要从这个里面获取进行获取
 * @copyright ChangChengLu
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    @Value("${mini-db-router.jdbc.datasource.default}")
    private String defaultDataSource;

    @Override
    protected Object determineCurrentLookupKey() {
        if (null == DBContextHolder.getDBKey()) {
            return defaultDataSource;
        } else {
            return "db" + DBContextHolder.getDBKey();
        }
    }
}

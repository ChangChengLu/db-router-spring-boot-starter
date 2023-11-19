package com.cclu.middleware.db.router.config;

import com.cclu.middleware.db.router.DBRouterConfig;
import com.cclu.middleware.db.router.DBRouterJoinPoint;
import com.cclu.middleware.db.router.dynamic.DynamicDataSource;
import com.cclu.middleware.db.router.dynamic.DynamicMybatisPlugin;
import com.cclu.middleware.db.router.strategy.IDBRouterStrategy;
import com.cclu.middleware.db.router.strategy.impl.DBRouterStrategyHashCode;
import com.cclu.middleware.db.router.util.PropertyUtil;
import com.cclu.middleware.db.router.util.StringUtils;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author ChangCheng Lu
 * @date 2023/11/11 16:18
 * @description 数据源配置解析
 * @copyright ChangChengLu
 *
 * setEnvironment: 获取并封装数据源配置信息
 * Interceptor: mybatis 插件加载
 * DataSource: 数据源加载
 * Initializing ExecutorService 'applicationTaskExecutor'
 * DBRouterConfigDB 配置加载
 * IDBRouterStrategy: 数据库路由策略加载
 * DBRouterJointPoint: 切面加载
 * TransactionTemplate: 事务配置加载
 */
@Configuration
public class DataSourceAutoConfig implements EnvironmentAware {

    /**
     * 分库全局属性
     */
    private static final String TAG_GLOBAL = "global";

    /**
     * 连接池属性
     */
    private static final String TAG_POOL = "pool";

    /**
     * 数据源配置组
     */
    private Map<String, Map<String, Object>> dataSourceMap = new HashMap<>();

    /**
     * 默认数据源配置
     */
    private Map<String, Object> defaultDataSourceConfig;

    /**
     * 分库数量
     */
    private int dbCount;

    /**
     * 分表数量
     */
    private int tbCount;

    /**
     * 路由字段
     */
    private String routerKey;

    /**
     * 加载切面
     * @param dbRouterConfig 分库分表配置信息
     * @param dbRouterStrategy 分库分表策略
     * @return DBRouterJoinPoint
     */
    @Bean(name = "db-router-point")
    @ConditionalOnMissingBean
    public DBRouterJoinPoint point(DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy) {
        return new DBRouterJoinPoint(dbRouterConfig, dbRouterStrategy);
    }

    /**
     * 返回分库分表配置信息类
     * @return 分库分表配置信息类
     */
    @Bean
    public DBRouterConfig dbRouterConfig() {
        return new DBRouterConfig(dbCount, tbCount, routerKey);
    }

    /**
     *
     * @return mybatis 插件
     */
    @Bean
    public Interceptor plugin() {
        return new DynamicMybatisPlugin();
    }

    private DataSource createDataSource(Map<String, Object> attributes) {
        try {
            DataSourceProperties dataSourceProperties = new DataSourceProperties();
            dataSourceProperties.setUrl(attributes.get("url").toString());
            dataSourceProperties.setUsername(attributes.get("username").toString());
            dataSourceProperties.setPassword(attributes.get("password").toString());

            String driverClassName = attributes.get("driver-class-name") == null ? "com.zaxxer.hikari.HikariDataSource" : attributes.get("driver-class-name").toString();
            dataSourceProperties.setDriverClassName(driverClassName);

            String typeClassName = attributes.get("type-class-name") == null ? "com.zaxxer.hikari.HikariDataSource" : attributes.get("type-class-name").toString();
            DataSource ds = dataSourceProperties.initializeDataSourceBuilder().type((Class<DataSource>) Class.forName(typeClassName)).build();

            MetaObject dsMeta = SystemMetaObject.forObject(ds);
            Map<String, Object> poolProps = (Map<String, Object>) (attributes.containsKey(TAG_POOL) ? attributes.get(TAG_POOL) : Collections.EMPTY_MAP);
            for (Map.Entry<String, Object> entry : poolProps.entrySet()) {
                // 中划线转驼峰
                String key = StringUtils.middleScoreToCamelCase(entry.getKey());
                if (dsMeta.hasSetter(key)) {
                    dsMeta.setValue(key, entry.getValue());
                }
            }
            return ds;
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("can not find datasource type class by class name", e);
        }
    }

    @Bean
    public DataSource createDataSource() {
        // 创建数据源
        Map<Object, Object> targetDataSources = new HashMap<>();
        for (String dbInfo : dataSourceMap.keySet()) {
            Map<String, Object> objMap = dataSourceMap.get(dbInfo);
            // 根据 objMap 创建 DataSourceProperties，遍历 objMap 根据书香反射创建 DataSourceProperties
            DataSource ds = createDataSource(objMap);
            targetDataSources.put(dbInfo, ds);
        }

        // 设置数据源
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSources);
        // db0 为默认数据源
        dynamicDataSource.setDefaultTargetDataSource(createDataSource(defaultDataSourceConfig));

        return dynamicDataSource;
    }

    @Bean
    public IDBRouterStrategy dbRouterStrategy(DBRouterConfig dbRouterConfig) {
        return new DBRouterStrategyHashCode(dbRouterConfig);
    }

    /**
     *
     * @param dataSource 数据源
     * @return 事务模板类
     */
    @Bean
    public TransactionTemplate transactionTemplate(DataSource dataSource) {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager();
        dataSourceTransactionManager.setDataSource(dataSource);

        TransactionTemplate transactionTemplate = new TransactionTemplate();
        transactionTemplate.setTransactionManager(dataSourceTransactionManager);
        transactionTemplate.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return transactionTemplate;
    }

    /**
     *
     * @param environment profile的抽象，获取配置文件配置信息
     */
    @Override
    public void setEnvironment(Environment environment) {
        // 配置文件前缀
        String prefix = "mini-db-router.jdbc.datasource.";
        // 解析分库数量
        dbCount = Integer.parseInt(Objects.requireNonNull(environment.getProperty(prefix + "dbCount")));
        // 解析分表数量
        tbCount = Integer.parseInt(Objects.requireNonNull(environment.getProperty(prefix + "tbCount")));
        // 路由字段(分区字段)
        routerKey = environment.getProperty(prefix + "routerKey");
        // 分库分表数据源
        String dataSources = environment.getProperty(prefix + "list");
        // prefix + TAG_GLOBAL = "mini-db-router.jdbc.datasource.global"
        Map<String, Object> globalInfo = getGlobalProps(environment, prefix + TAG_GLOBAL);
        for (String dbInfo : dataSources.split(",")) {
            // prefix + dbInfo e.g: mini-db-router.jdbc.datasource.db0、mini-db-router.jdbc.datasource.db1
            final String dbPrefix = prefix + dbInfo;
            // 将 prefix + dbInfo 前缀属性封装成 Map
            Map<String, Object> dataSourceProps = PropertyUtil.handle(environment, dbPrefix, Map.class);
            // 将全局配置信息注入默认数据库配置信息(不需要额外维护全局配置信息)
            injectGlobal(dataSourceProps, globalInfo);
            // 以 dbInfo(数据库标识号) 为 Key，存放对应的数据源配置信息
            dataSourceMap.put(dbInfo, dataSourceProps);
        }
        // 获取默认数据源(默认数据库)
        // prefix + "default" e.g: mini-db-router.jdbc.datasource.default=db00
        String defaultData = environment.getProperty(prefix + "default");
        // 获取默认数据库具体配置，如URL、账户、密码等
        defaultDataSourceConfig = PropertyUtil.handle(environment, prefix + defaultData, Map.class);
        injectGlobal(defaultDataSourceConfig, globalInfo);
    }

    /**
     * 同组同级配置Map对象封装
     * @param environment profile文件抽象
     * @param key profile文件内具体配置前缀
     * @return 同组同级配置Map对象封装
     */
    private Map<String, Object> getGlobalProps(Environment environment, String key) {
        try {
            return PropertyUtil.handle(environment, key, Map.class);
        } catch (Exception e) {
            return Collections.EMPTY_MAP;
        }
    }

    /**
     * 将全局配置信息注入数据源配置信息
     * @param origin dataSourceProps 数据源配置信息
     * @param global globalInfo 全局配置信息(全局配置信息对任意一个数据源配置均有效)
     */
    private void injectGlobal(Map<String, Object> origin, Map<String, Object> global) {
        for (String key : global.keySet()) {
            if (!origin.containsKey(key)) {
                origin.put(key, global.get(key));
            } else if (origin.get(key) instanceof Map) {
                injectGlobal((Map<String, Object>) origin.get(key), (Map<String, Object>) global.get(key));
            }
        }
    }
}

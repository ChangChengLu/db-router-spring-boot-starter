package com.cclu.middleware.db.router.dynamic;

import com.cclu.middleware.db.router.DBContextHolder;
import com.cclu.middleware.db.router.annotation.DBRouterStrategy;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ChangCheng Lu
 * @date 2023/11/16 11:45
 * @description Mybatis 拦截器，通过对 SQL 语句的拦截处理，修改分表信息。
 * @copyright ChangChengLu
 */
// 指定拦截的目标方法
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class DynamicMybatisPlugin implements Interceptor {

    /**
     * 对应查询、插入、更新操作，匹配操作的表名
     */
    private Pattern pattern = Pattern.compile("(from|into|update)[\\s]{1,}(\\w{1,})", Pattern.CASE_INSENSITIVE);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取StatementHandler(指定拦截的目标对象): 准备(预编译)和执行SQL
        // 此处拦截的 statementHandler 实现类实际上是 RoutingStatementHandler
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        /*
          MetaObject 是 MyBatis 中的一个工具类，用于简化和统一对象属性的访问操作。

          这段代码的作用是创建一个MetaObject对象，并将statementHandler对象作为参数传入。MetaObject是MyBatis框架中的一个工具类，用于方便地操作Java对象的属性和方法。

          解释每个参数的作用:
          statementHandler：需要进行操作的目标对象。
          SystemMetaObject.DEFAULT_OBJECT_FACTORY：指定了使用默认的对象工厂，用于创建MetaObject对象。
          SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY：指定了使用默认的对象包装器工厂，用于包装和管理Java对象。
          DefaultReflectorFactory：使用默认的反射工厂，用于获取并操作Java对象的属性和方法。

          通过这段代码，我们可以使用MetaObject对象来访问和修改statementHandler对象的属性和方法，实现对其的拦截和处理。
         */
        MetaObject metaObject = MetaObject.forObject(statementHandler, SystemMetaObject.DEFAULT_OBJECT_FACTORY, SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY, new DefaultReflectorFactory());
        /*
          从 RoutingStatementHandler 对象中的 delegate 属性获取  mappedStatement 对象。

          RoutingStatementHandler 是 MyBatis 框架中的一个处理器，它用于根据 SQL 语句的类型和执行方式，将任务分发给不同的 StatementHandler 进行具体的处理。
          1. 路由功能：根据 SQL 语句的类型，将任务路由给对应的 StatementHandler 进行处理。例如，如果是查询语句，则会路由给 ResultSetHandler 处理结果集；如果是更新语句，则会路由给 PreparedStatementHandler 进行更新操作。
          2. 语句处理委派：根据路由结果，将任务委派给对应的 StatementHandler 进行具体的 SQL 语句执行和结果处理。
          3. 支持多种数据库：RoutingStatementHandler 具有适配不同数据库的能力，它可以根据配置的数据库类型选择合适的 StatementHandler 进行处理，以确保在不同数据库中的 SQL 执行和结果处理的兼容性。
          4. 提供统一的接口：RoutingStatementHandler 继承自 BaseStatementHandler，它提供了统一的接口和基础功能，方便其他类型的 StatementHandler 进行扩展和定制。
         */
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        // 获取自定义注解判断是否进行分表操作
        // 代表一个 XML 配置中映射的 SQL 语句
        // id属性: namespace + SQL语句的ID
        // <mapper namespace="com.cclu.lottery.infrastructure.dao.IActivityDao">
        // <insert id="insert" parameterType="com.cclu.lottery.infrastructure.po.Activity">
        // id: com.cclu.lottery.infrastructure.dao.IActivityDao.insert
        // 此处根据 id 属性获取类的全反射名称
        String id = mappedStatement.getId();
        String className = id.substring(0, id.lastIndexOf("."));
        Class<?> clazz = Class.forName(className);
        /*
          @Mapper
         * @DBRouterStrategy(splitTable = true)
         * public interface IUserStrategyExportDao {}
         */
        DBRouterStrategy dbRouterStrategy = clazz.getAnnotation(DBRouterStrategy.class);
        // 判断该类是否是存在 DBRouterStrategy 注解
        if (null == dbRouterStrategy || !dbRouterStrategy.splitTable()) {
            return invocation.proceed();
        }

        // 获取SQL
        // BoundSql 包含最终生成的 SQL，可能是静态生成的SQL，也可能是 Mybatis 动态生成的SQL
        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = boundSql.getSql();

        // 替换 SQL 表名 USER 为 USER_03
        Matcher matcher = pattern.matcher(sql);
        String tableName = null;
        if (matcher.find()) {
            tableName = matcher.group().trim();
        }
        assert null != tableName;
        String replaceSql = matcher.replaceAll(tableName + "_" + DBContextHolder.getTBKey());

        // 通过反射修改 SQL 语句
        Field field = boundSql.getClass().getDeclaredField("sql");
        field.setAccessible(true);
        field.set(boundSql, replaceSql);
        field.setAccessible(false);

        return invocation.proceed();
    }
}

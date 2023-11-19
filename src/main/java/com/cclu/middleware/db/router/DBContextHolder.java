package com.cclu.middleware.db.router;

/**
 * @author ChangCheng Lu
 * @date 2023/11/11 16:07
 * @description 数据源上下文
 * @copyright ChangChengLu
 */
public class DBContextHolder {

    /**
     * 数据库名称
     */
    private static final ThreadLocal<String> dbKey = new ThreadLocal<>();

    /**
     * 表名称
     */
    private static final ThreadLocal<String> tbKey = new ThreadLocal<>();

    public static void setDBKey(String dbKeyIdx) {
        dbKey.set(dbKeyIdx);
    }

    public static String getDBKey() {
        return dbKey.get();
    }

    public static void setTBKey(String tbKeyIdx) {
        tbKey.set(tbKeyIdx);
    }

    public static String getTBKey() {
        return tbKey.get();
    }

    public static void clearDBKey() {
        dbKey.remove();
    }

    public static void clearTBKey() {
        tbKey.remove();
    }
}
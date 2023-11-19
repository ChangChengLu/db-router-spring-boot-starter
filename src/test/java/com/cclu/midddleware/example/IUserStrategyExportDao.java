package com.cclu.midddleware.example;

import com.cclu.middleware.db.router.annotation.DBRouter;
import com.cclu.middleware.db.router.annotation.DBRouterStrategy;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author ChangCheng Lu
 * @date 2023/11/10 8:41
 * @description 用户策略计算结果Dao
 * @copyright ChangChengLu
 */
@Mapper
@DBRouterStrategy(splitTable = true)
public interface IUserStrategyExportDao {

    /**
     * 新增数据
     * @param userStrategyExport 用户策略
     */
    @DBRouter(key = "uId")
    void insert(UserStrategyExport userStrategyExport);

    /**
     * 查询数据
     * @param uId 用户ID
     * @return 用户策略
     */
    @DBRouter
    UserStrategyExport queryUserStrategyExportByUId(String uId);

}

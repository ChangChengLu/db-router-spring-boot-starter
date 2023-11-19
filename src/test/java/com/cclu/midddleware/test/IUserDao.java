package com.cclu.midddleware.test;

import com.cclu.middleware.db.router.annotation.DBRouter;

/**
 * @author ChangCheng Lu
 * @date 2023/11/16 13:06
 * @description 用户接口
 * @copyright ChangChengLu
 */
public interface IUserDao {

    @DBRouter(key = "userId")
    void insertUser(String req);

}

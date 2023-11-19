package com.cclu.middleware.db.router;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ChangCheng Lu
 * @date 2023/11/11 20:10
 * @description 数据路由配置
 * @copyright ChangChengLu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DBRouterConfig {

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

}

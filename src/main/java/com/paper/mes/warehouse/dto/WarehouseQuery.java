package com.paper.mes.warehouse.dto;

import lombok.Data;

/**
 * 仓库列表查询入参。
 */
@Data
public class WarehouseQuery {

    /** 关键字：命中仓库编码或名称 */
    private String keyword;
    /** 按状态筛选：1启用 2停用，空为全部 */
    private Integer status;

    private long current = 1;
    private long size = 10;
}

package com.paper.mes.machine.dto;

import lombok.Data;

/**
 * 机台列表查询入参。
 */
@Data
public class MachineQuery {

    /** 关键字：命中机台编码或名称 */
    private String keyword;
    /** 按状态筛选：1启用 2停用，空为全部 */
    private Integer status;

    private long current = 1;
    private long size = 10;
}

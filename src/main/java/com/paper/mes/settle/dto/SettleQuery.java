package com.paper.mes.settle.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 结算单列表查询入参。
 */
@Data
public class SettleQuery {

    /** 关键字：命中结算单号或客户名 */
    private String keyword;
    private String customerUuid;
    /** 1待结算 2部分收款 3全部结清 */
    private Integer settleStatus;
    /** 1按单 2按月批量 */
    private Integer settleType;
    private LocalDate dateFrom;
    private LocalDate dateTo;

    private long current = 1;
    private long size = 10;
}

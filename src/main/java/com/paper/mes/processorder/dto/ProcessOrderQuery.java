package com.paper.mes.processorder.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 加工单列表查询入参，支持状态/日期/客户筛选。
 */
@Data
public class ProcessOrderQuery {

    /** 关键字：命中加工单号或客户名 */
    private String keyword;

    /** 订单状态 0草稿 1待下发 2加工中 3待回录 4已完成 5已结算 6已作废 */
    private Integer orderStatus;

    /** 客户UUID */
    private String customerUuid;

    /** 制单日期起（含） */
    private LocalDate dateFrom;
    /** 制单日期止（含） */
    private LocalDate dateTo;

    private long current = 1;
    private long size = 10;
}

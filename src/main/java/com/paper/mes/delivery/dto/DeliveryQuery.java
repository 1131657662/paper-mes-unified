package com.paper.mes.delivery.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 出库单列表查询入参。
 */
@Data
public class DeliveryQuery {

    /** 关键字：命中出库单号或客户名 */
    private String keyword;
    private String customerUuid;
    /** 1待出库 2已出库签收 3已作废 */
    private Integer deliveryStatus;
    private LocalDate dateFrom;
    private LocalDate dateTo;

    private long current = 1;
    private long size = 10;
}

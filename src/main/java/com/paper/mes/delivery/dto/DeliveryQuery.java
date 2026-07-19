package com.paper.mes.delivery.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 出库单列表查询入参。
 */
@Data
public class DeliveryQuery {

    /** 关键字：命中出库单号或客户名 */
    @Size(max = 100, message = "查询关键字不能超过100个字符")
    private String keyword;
    @Size(max = 64, message = "客户标识不能超过64个字符")
    private String customerUuid;
    /** 1待出库 2已出库签收 3已作废 */
    @Min(value = 1, message = "出库状态无效")
    @Max(value = 3, message = "出库状态无效")
    private Integer deliveryStatus;
    private LocalDate dateFrom;
    private LocalDate dateTo;

    @Min(value = 1, message = "页码不能小于1")
    private long current = 1;
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private long size = 10;
}

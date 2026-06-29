package com.paper.mes.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户维度统计：按客户分组的单量/吨位/刀数/金额。
 */
@Data
public class CustomerReportVO {

    private String customerUuid;

    private String customerName;

    /** 加工单数 */
    private Long orderCount;

    /** 原纸总吨 */
    private BigDecimal totalTon;

    /** 实际总刀数 */
    private Long totalKnife;

    /** 整单总金额合计 */
    private BigDecimal totalAmount;
}

package com.paper.mes.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 月度加工汇总：按制单月份分组的吨位/刀数/金额统计。
 */
@Data
public class MonthlyReportVO {

    /** 月份 yyyy-MM */
    private String month;

    /** 加工单数 */
    private Long orderCount;

    /** 原纸总吨 */
    private BigDecimal totalTon;

    /** 实际总刀数 */
    private Long totalKnife;

    /** 整单总金额合计 */
    private BigDecimal totalAmount;

    /** 成品总重 kg */
    private BigDecimal totalFinishWeight;
}

package com.paper.mes.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 损耗分析：按制单月份分组的原纸卷损耗统计。
 */
@Data
public class LossReportVO {

    /** 月份 yyyy-MM */
    private String month;

    /** 原纸卷数 */
    private Long rollCount;

    /** 原纸实际总重 kg（SUM actual_weight） */
    private BigDecimal totalOriginalWeight;

    /** 总损耗 kg */
    private BigDecimal totalLossWeight;

    /** 平均损耗率 % */
    private BigDecimal avgLossRatio;
}

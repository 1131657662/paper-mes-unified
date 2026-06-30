package com.paper.mes.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 报表总览指标。
 */
@Data
public class ReportOverviewVO {

    private Long orderCount;
    private Long originalRollCount;
    private Long finishRollCount;
    private BigDecimal originalWeight;
    private BigDecimal finishWeight;
    private BigDecimal lossWeight;
    private BigDecimal lossRatio;
    private Long knifeCount;
    private BigDecimal sawAmount;
    private BigDecimal rewindAmount;
    private BigDecimal processAmount;
    private BigDecimal extraAmount;
    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;
    private BigDecimal unreceivedAmount;
}

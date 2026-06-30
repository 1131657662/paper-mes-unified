package com.paper.mes.report.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户、产品、工艺、机台等维度汇总。
 */
@Data
public class ReportDimensionVO {

    private String dimensionKey;
    private String dimensionName;
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
    private BigDecimal settledAmount;
    private BigDecimal pendingSettleAmount;
    private BigDecimal receivedAmount;
    private BigDecimal unreceivedAmount;
}

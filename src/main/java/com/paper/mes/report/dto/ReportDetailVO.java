package com.paper.mes.report.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 报表加工单明细行。
 */
@Data
public class ReportDetailVO {

    private String orderUuid;
    private String orderNo;
    private LocalDate orderDate;
    private LocalDate accountingDate;
    private String customerName;
    private Integer settleType;
    private Integer isInvoice;
    private Integer orderStatus;
    private Long originalRollCount;
    private Long finishRollCount;
    private String paperSummary;
    private String processSummary;
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
    private BigDecimal cashReceivedAmount;
    private BigDecimal scrapOffsetAmount;
    private BigDecimal unreceivedAmount;
}

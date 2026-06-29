package com.paper.mes.settle.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 结算工作台候选加工单视图。
 */
@Data
public class SettleCandidateVO {

    private String orderUuid;
    private String orderNo;
    private String customerUuid;
    private String customerName;
    private LocalDate orderDate;
    /** 1次结 2月结，来自加工单快照。 */
    private Integer settleType;
    private Integer settleDay;
    private Integer isInvoice;
    private Integer originalRollCount;
    private BigDecimal originalRollWeight;
    private Integer finishRollCount;
    private BigDecimal finishRollWeight;
    private BigDecimal sawAmount;
    private BigDecimal rewindAmount;
    private BigDecimal extraAmount;
    private BigDecimal totalAmount;
}

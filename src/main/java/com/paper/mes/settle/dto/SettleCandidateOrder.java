package com.paper.mes.settle.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Narrow database projection used by the settlement candidate list. */
@Data
public class SettleCandidateOrder {

    private String uuid;
    private String orderNo;
    private String customerUuid;
    private String customerName;
    private LocalDate orderDate;
    private LocalDate accountingDate;
    private Integer settleType;
    private Integer settleDay;
    private Integer isInvoice;
    private BigDecimal totalExtraAmount;
    private BigDecimal totalAmount;
}

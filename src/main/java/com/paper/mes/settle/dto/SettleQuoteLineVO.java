package com.paper.mes.settle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class SettleQuoteLineVO {
    private String orderUuid;
    private BigDecimal sawAmount;
    private BigDecimal rewindAmount;
    private BigDecimal extraAmount;
    private BigDecimal amountNoTax;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
}

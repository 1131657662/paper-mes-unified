package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DraftSummaryVO {

    private String orderUuid;
    private String orderNo;
    private String customerName;
    private LocalDate orderDate;
    private Integer currentStep;
    private Integer rollCount;
    private Integer configuredCount;
    private BigDecimal totalWeight;
}

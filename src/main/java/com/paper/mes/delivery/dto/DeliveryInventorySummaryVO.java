package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DeliveryInventorySummaryVO {

    private long customerCount;
    private long totalRollCount;
    private long availableRollCount;
    private long lockedRollCount;
    private long productRollCount;
    private long remainRollCount;
    private long directRollCount;
    private BigDecimal productWeight;
    private BigDecimal remainWeight;
    private BigDecimal directWeight;
    private long productAvailableRollCount;
    private long remainAvailableRollCount;
    private long directAvailableRollCount;
    private BigDecimal productAvailableWeight;
    private BigDecimal remainAvailableWeight;
    private BigDecimal directAvailableWeight;
    private BigDecimal totalWeight;
    private BigDecimal availableWeight;
    private BigDecimal lockedWeight;
    private BigDecimal plannedOutWeight;
    private long stockInTimeUnknownCount;
    private LocalDateTime asOf;
}

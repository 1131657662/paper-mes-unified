package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DeliveryInventoryCustomerVO {

    private String customerUuid;
    private String customerName;
    private long totalRollCount;
    private long availableRollCount;
    private long lockedRollCount;
    private BigDecimal totalWeight;
    private BigDecimal availableWeight;
    private BigDecimal lockedWeight;
    private BigDecimal plannedOutWeight;
    private LocalDateTime oldestStockInTime;
    private long stockInTimeUnknownCount;
    private int warehouseCount;
    private String warehouseNames;
    private String paperNames;
}

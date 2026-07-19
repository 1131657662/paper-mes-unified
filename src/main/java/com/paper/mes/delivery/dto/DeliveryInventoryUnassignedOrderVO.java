package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DeliveryInventoryUnassignedOrderVO {

    private String orderUuid;
    private String orderNo;
    private LocalDate orderDate;
    private Integer orderStatus;
    private String customerUuid;
    private String customerName;
    private long unassignedRollCount;
    private BigDecimal unassignedWeight;
    private String knownWarehouseUuid;
    private String knownWarehouseName;
    private boolean warehouseConflict;
    private long activeLockCount;
}

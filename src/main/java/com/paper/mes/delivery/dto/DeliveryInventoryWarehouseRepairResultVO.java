package com.paper.mes.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeliveryInventoryWarehouseRepairResultVO {

    private int repairedOrderCount;
    private int repairedRollCount;
    private String warehouseUuid;
    private String warehouseName;
}

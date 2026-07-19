package com.paper.mes.delivery.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeliveryInventoryCustomerQuery extends DeliveryInventoryFilter {

    @Min(value = 1, message = "页码不能小于1")
    private long current = 1;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private long size = 20;
}

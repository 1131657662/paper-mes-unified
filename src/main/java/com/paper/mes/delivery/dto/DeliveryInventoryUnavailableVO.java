package com.paper.mes.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeliveryInventoryUnavailableVO {

    private String finishUuid;
    private String reason;
}

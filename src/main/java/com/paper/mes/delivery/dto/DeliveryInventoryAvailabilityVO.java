package com.paper.mes.delivery.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeliveryInventoryAvailabilityVO {

    private List<String> availableFinishUuids = new ArrayList<>();
    private List<DeliveryInventoryUnavailableVO> unavailable = new ArrayList<>();
}

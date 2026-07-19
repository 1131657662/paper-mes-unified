package com.paper.mes.delivery.dto;

import lombok.Data;

@Data
public class AvailableFinishStatsVO {
    private long productCount;
    private long remainCount;
    private long unassignedWarehouseCount;
    private long otherWarehouseCount;
    private long lockedCount;
    private long invalidWeightCount;
}

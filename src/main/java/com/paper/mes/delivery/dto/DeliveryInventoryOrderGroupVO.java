package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class DeliveryInventoryOrderGroupVO {

    private String orderUuid;
    private String orderNo;
    private LocalDate orderDate;
    private long totalRollCount;
    private BigDecimal totalWeight;
    private long availableRollCount;
    private long lockedRollCount;
    private List<DeliveryInventoryFinishVO> finishes = new ArrayList<>();
}

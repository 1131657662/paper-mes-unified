package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DeliveryCustomerRevisionSummaryVO {
    private String uuid;
    private String deliveryUuid;
    private Integer revisionNo;
    private String reason;
    private Integer itemCount;
    private BigDecimal customerTotalWeight;
    private String operator;
    private LocalDateTime createdAt;
}

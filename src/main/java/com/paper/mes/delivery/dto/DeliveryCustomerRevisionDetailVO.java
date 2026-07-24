package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeliveryCustomerRevisionDetailVO {
    private String uuid;
    private String deliveryUuid;
    private Integer revisionNo;
    private String reason;
    private Integer itemCount;
    private BigDecimal customerTotalWeight;
    private String operator;
    private LocalDateTime createdAt;
    private List<DeliveryCustomerRevisionItemVO> items;
}

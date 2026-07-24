package com.paper.mes.delivery.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DeliveryCustomerRevisionPreviewVO {

    private String deliveryUuid;
    private String deliveryNo;
    private Integer deliveryVersion;
    private Integer deliveryStatus;
    private Integer currentRevisionNo;
    private String currentRevisionKind;
    private Integer nextRevisionNo;
    private Integer itemCount;
    private Integer validItemCount;
    private BigDecimal physicalTotalWeight;
    private BigDecimal customerTotalWeight;
    private BigDecimal differenceWeight;
    private boolean hasErrors;
    private List<DeliveryCustomerSpecVO> items;
}

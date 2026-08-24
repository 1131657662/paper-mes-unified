package com.paper.mes.remain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RemainInventoryVO {

    private String lotUuid;
    private String registrationUuid;
    private String registrationNo;
    private String registrationLineUuid;
    private String sourceFinishRollUuid;
    private String customerUuid;
    private String warehouseUuid;
    private BigDecimal currentWeight;
    private String status;
    private String priceStatus;
}

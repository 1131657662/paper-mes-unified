package com.paper.mes.remain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RemainApplicationVO {

    private String uuid;
    private String registrationUuid;
    private String settleUuid;
    private String adjustmentUuid;
    private String receiveUuid;
    private String status;
    private BigDecimal amount;
    private BigDecimal weight;
}

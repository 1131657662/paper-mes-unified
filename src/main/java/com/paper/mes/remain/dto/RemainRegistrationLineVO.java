package com.paper.mes.remain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RemainRegistrationLineVO {

    private String uuid;
    private String sourceFinishRollUuid;
    private BigDecimal sourceSystemWeight;
    private BigDecimal transferredSystemWeight;
    private BigDecimal rolledBackSystemWeight;
    private BigDecimal processedSystemWeight;
    private BigDecimal currentOwnWeight;
    private BigDecimal amount;
    private String status;
}

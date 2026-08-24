package com.paper.mes.remain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RemainAdjustmentLineVO {
    private String registrationLineUuid;
    private BigDecimal amount;
    private BigDecimal weight;
}

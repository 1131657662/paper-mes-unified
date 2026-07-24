package com.paper.mes.customer.dto;

import java.math.BigDecimal;

public record CustomerProcessPriceVO(
        String catalogUuid,
        Integer stepType,
        String processCode,
        String processName,
        String billingBasis,
        String billingUnitName,
        BigDecimal price,
        boolean defaultOption) {
}

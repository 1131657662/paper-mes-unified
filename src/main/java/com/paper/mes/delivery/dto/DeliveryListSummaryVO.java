package com.paper.mes.delivery.dto;

import java.math.BigDecimal;

public record DeliveryListSummaryVO(
        long totalDocumentCount,
        long pendingDocumentCount,
        long deliveredDocumentCount,
        long voidDocumentCount,
        long activeRollCount,
        BigDecimal activeWeight,
        BigDecimal pendingWeight,
        BigDecimal deliveredWeight) {
}

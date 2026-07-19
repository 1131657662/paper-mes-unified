package com.paper.mes.settle.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettleListSummaryVO(
        long totalDocumentCount,
        long pendingDocumentCount,
        long partialDocumentCount,
        long paidDocumentCount,
        long voidDocumentCount,
        BigDecimal activeTotalAmount,
        BigDecimal activeReceivedAmount,
        BigDecimal activeUnreceivedAmount,
        BigDecimal activeDiscountAmount,
        LocalDateTime asOf) {
}

package com.paper.mes.settle.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SettleCollectionSummaryVO(
        long dueTodayCount,
        BigDecimal dueTodayAmount,
        long overdueCount,
        BigDecimal overdueAmount,
        long upcomingCount,
        BigDecimal upcomingAmount,
        long remindedTodayCount,
        BigDecimal remindedTodayAmount,
        LocalDateTime asOf) {
}

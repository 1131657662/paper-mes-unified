package com.paper.mes.report.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReportDeliveryAnalysisVO(
        String topicCode, Overview overview, List<Dimension> monthlyTrend,
        List<Dimension> warehouseBreakdown, LocalDateTime asOf) {
    @Data
    public static class Overview {
        private Long documentCount;
        private Long pendingDocuments;
        private Long completedDocuments;
        private Long rollCount;
        private BigDecimal totalWeight;
        private BigDecimal pendingWeight;
        private BigDecimal completedWeight;
    }

    @Data
    public static class Dimension {
        private String dimensionKey;
        private String dimensionName;
        private Long documentCount;
        private Long rollCount;
        private BigDecimal totalWeight;
        private BigDecimal completedWeight;
    }
}

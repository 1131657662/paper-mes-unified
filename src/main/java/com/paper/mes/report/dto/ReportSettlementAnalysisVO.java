package com.paper.mes.report.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReportSettlementAnalysisVO(
        String topicCode, Overview overview, List<Dimension> monthlyTrend,
        List<Dimension> customerBreakdown, LocalDateTime asOf) {
    @Data
    public static class Overview {
        private Long totalDocuments;
        private Long pendingDocuments;
        private Long partialDocuments;
        private Long overdueDocuments;
        private BigDecimal totalAmount;
        private BigDecimal receivedAmount;
        private BigDecimal unreceivedAmount;
        private BigDecimal overdueAmount;
    }

    @Data
    public static class Dimension {
        private String dimensionKey;
        private String dimensionName;
        private Long documentCount;
        private BigDecimal totalAmount;
        private BigDecimal receivedAmount;
        private BigDecimal unreceivedAmount;
    }
}

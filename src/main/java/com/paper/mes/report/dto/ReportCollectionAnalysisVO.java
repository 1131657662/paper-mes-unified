package com.paper.mes.report.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReportCollectionAnalysisVO(
        String topicCode, Overview overview, List<Dimension> monthlyTrend,
        List<Dimension> customerBreakdown, LocalDateTime asOf) {
    @Data
    public static class Overview {
        private Long recordCount;
        private Long cashRecordCount;
        private Long scrapRecordCount;
        private Long discountRecordCount;
        private BigDecimal settledAmount;
        private BigDecimal cashAmount;
        private BigDecimal scrapOffsetAmount;
        private BigDecimal discountAmount;
        private BigDecimal scrapWeight;
    }

    @Data
    public static class Dimension {
        private String dimensionKey;
        private String dimensionName;
        private Long recordCount;
        private BigDecimal settledAmount;
        private BigDecimal cashAmount;
        private BigDecimal nonCashAmount;
        private BigDecimal scrapWeight;
    }
}

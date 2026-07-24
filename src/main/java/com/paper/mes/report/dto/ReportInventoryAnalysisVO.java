package com.paper.mes.report.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReportInventoryAnalysisVO(
        String topicCode, String timelineMode, Overview overview, List<Dimension> stockInCohorts,
        List<Dimension> warehouseBreakdown, LocalDateTime asOf) {
    @Data
    public static class Overview {
        private Long rollCount;
        private Long availableRollCount;
        private Long lockedRollCount;
        private Long exceptionRollCount;
        private BigDecimal totalWeight;
        private BigDecimal lockedWeight;
    }

    @Data
    public static class Dimension {
        private String dimensionKey;
        private String dimensionName;
        private Long rollCount;
        private BigDecimal totalWeight;
        private BigDecimal lockedWeight;
    }
}

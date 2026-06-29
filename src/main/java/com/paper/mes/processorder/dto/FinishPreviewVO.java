package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class FinishPreviewVO {

    private String originalUuid;
    private Integer rewindMode;
    private Integer originalWidth;
    private int finishCount;
    private int trimCount;
    private Integer spareCount;
    private BigDecimal totalEstimateWeight;
    private BigDecimal totalTrimWeight;
    private List<SegmentPreview> segments;
    private List<FinishItemPreview> finishes;

    @Data
    public static class SegmentPreview {
        private Integer segmentSort;
        private BigDecimal segmentRatio;
        private Integer targetDiameter;
        private Integer repeatCount;
        private Integer layoutWidth;
        private Integer trimWidth;
        private String summary;
    }

    @Data
    public static class FinishItemPreview {
        private Integer segmentSort;
        private Integer finishWidth;
        private Integer finishDiameter;
        private Integer finishCoreDiameter;
        private BigDecimal segmentRatio;
        private BigDecimal estimateWeight;
        private Integer trimWidth;
        private BigDecimal trimWeight;
        private String sourceSummary;
    }
}

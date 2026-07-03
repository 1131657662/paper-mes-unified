package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 后续/多段工艺路线预览结果。
 */
@Data
public class ProcessRoutePreviewVO {

    private String originalUuid;
    private BigDecimal totalAmount;
    private List<RouteStageLineVO> stages;
    private List<RouteOutputVO> outputs;

    @Data
    public static class RouteStageLineVO {
        private Integer stageLevel;
        private Integer stepType;
        private String stepName;
        private List<String> inputOutputKeys;
        private Integer knifeCount;
        private BigDecimal processWeight;
        private BigDecimal unitPrice;
        private BigDecimal stepAmount;
    }

    @Data
    public static class RouteOutputVO {
        private String outputKey;
        private Integer stageLevel;
        private Integer outputSort;
        private Integer outputType;
        private Boolean consumedByNextStage;
        private String paperName;
        private Integer gramWeight;
        private Integer finishWidth;
        private Integer finishDiameter;
        private Integer finishCoreDiameter;
        private BigDecimal estimateWeight;
        private String remark;
    }
}

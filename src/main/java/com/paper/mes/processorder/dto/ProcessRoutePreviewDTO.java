package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 后续/多段工艺路线预览入参。
 */
@Data
public class ProcessRoutePreviewDTO {

    @NotBlank(message = "原纸卷UUID不能为空")
    private String originalUuid;

    @Valid
    @NotEmpty(message = "工艺路线不能为空")
    private List<RouteStageDTO> stages;

    @Data
    public static class RouteStageDTO {
        @NotNull(message = "阶段层级不能为空")
        @Min(value = 1, message = "阶段层级至少为1")
        private Integer stageLevel;

        private List<String> inputOutputKeys;

        @NotNull(message = "工序类型不能为空")
        private Integer stepType;

        private String stepName;
        private String machineUuid;
        private Integer knifeCount;
        private BigDecimal processWeight;
        private BigDecimal unitPrice;
        private ProcessPlanDTO plan;

        @Valid
        private List<RouteOutputDTO> outputs;
    }

    @Data
    public static class RouteOutputDTO {
        private String outputKey;
        private Integer outputType;
        private Integer count;
        /** 0正品 1边角余料。 */
        private Integer isRemain;
        private String paperName;
        private Integer gramWeight;
        private Integer finishWidth;
        private Integer finishDiameter;
        private Integer finishCoreDiameter;
        private BigDecimal estimateWeight;
        private String remark;
    }
}

package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RewindPlanPreviewDTO {

    @NotNull(message = "复卷模式不能为空")
    private Integer rewindMode;

    @Min(value = 0, message = "备用号数量不能小于0")
    @Max(value = 500, message = "备用号数量不能超过500")
    private Integer spareCount;

    @Valid
    @Size(max = 100, message = "复卷段不能超过100条")
    private List<RewindSegmentDTO> segments;

    @Data
    public static class RewindSegmentDTO {
        private Integer segmentSort;
        private BigDecimal segmentRatio;
        private Integer targetDiameter;
        private Integer finishCoreDiameter;
        @Min(value = 1, message = "分段重复次数至少为1")
        @Max(value = 500, message = "分段重复次数不能超过500")
        private Integer repeatCount;

        @Valid
        @Size(max = 100, message = "复卷来源不能超过100条")
        private List<FinishConfigSpecDTO.FinishSourceDTO> sources;

        @Valid
        @Size(max = 500, message = "复卷排版项不能超过500条")
        private List<RewindLayoutItemDTO> layoutItems;
    }

    @Data
    public static class RewindLayoutItemDTO {
        @NotNull(message = "门幅不能为空")
        @Min(value = 1, message = "门幅必须大于0")
        private Integer width;

        @Min(value = 1, message = "数量至少为1")
        @Max(value = 500, message = "单个排版数量不能超过500")
        private Integer quantity;

        private String itemType;

        @Size(max = 100, message = "客户品名不能超过100个字符")
        private String customerPaperName;
        private Integer customerGramWeight;
        private Integer customerFinishWidth;
        @Size(max = 255, message = "客户规格改写原因不能超过255个字符")
        private String customerSpecOverrideReason;

        @Valid
        @Size(max = 100, message = "复卷层不能超过100层")
        private List<FinishConfigSpecDTO.FinishLayerDTO> layers;
    }
}

package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RewindPlanPreviewDTO {

    @NotNull(message = "复卷模式不能为空")
    private Integer rewindMode;

    @Min(value = 0, message = "备用号数量不能小于0")
    private Integer spareCount;

    @Valid
    private List<RewindSegmentDTO> segments;

    @Data
    public static class RewindSegmentDTO {
        private Integer segmentSort;
        private BigDecimal segmentRatio;
        private Integer targetDiameter;
        private Integer finishCoreDiameter;
        private Integer repeatCount;

        @Valid
        private List<FinishConfigSpecDTO.FinishSourceDTO> sources;

        @Valid
        private List<RewindLayoutItemDTO> layoutItems;
    }

    @Data
    public static class RewindLayoutItemDTO {
        @NotNull(message = "门幅不能为空")
        @Min(value = 1, message = "门幅必须大于0")
        private Integer width;

        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;

        private String itemType;

        @Valid
        private List<FinishConfigSpecDTO.FinishLayerDTO> layers;
    }
}

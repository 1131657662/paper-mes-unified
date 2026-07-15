package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单卷成品规格。
 */
@Data
public class FinishConfigSpecDTO {

    /**
     * FINISH=成品，TRIM=切边/修边。为空时兼容旧数据，按成品处理。
     */
    private String itemType;

    private Integer finishWidth;
    private Integer finishDiameter;
    private Integer finishCoreDiameter;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer count;

    @PositiveOrZero(message = "预估重量不能为负")
    private BigDecimal estimateWeight;
    @DecimalMin(value = "0.00", message = "分摊比例不能为负")
    @DecimalMax(value = "100.00", message = "分摊比例不能超过100%")
    private BigDecimal splitRatio;

    @Valid
    @Size(max = 100, message = "成品来源不能超过100条")
    private List<FinishSourceDTO> sources;

    @Valid
    @Size(max = 100, message = "成品层不能超过100层")
    private List<FinishLayerDTO> layers;

    @Data
    public static class FinishSourceDTO {
        private String originalUuid;
        @DecimalMin(value = "0.00", message = "来源占比不能为负")
        @DecimalMax(value = "100.00", message = "来源占比不能超过100%")
        private BigDecimal shareRatio;
        @DecimalMin(value = "0.00", message = "消耗比例不能为负")
        @DecimalMax(value = "100.00", message = "消耗比例不能超过100%")
        private BigDecimal consumeRatio;
    }

    @Data
    public static class FinishLayerDTO {
        @Positive(message = "分层外径必须大于0")
        private Integer outDiameter;
        @Positive(message = "分层纸芯必须大于0")
        private Integer coreDiameter;
    }
}

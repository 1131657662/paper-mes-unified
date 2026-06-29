package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
    @Min(value = 1, message = "数量至少为 1")
    private Integer count;

    private BigDecimal estimateWeight;
    private BigDecimal splitRatio;

    @Valid
    private List<FinishSourceDTO> sources;

    @Valid
    private List<FinishLayerDTO> layers;

    @Data
    public static class FinishSourceDTO {
        private String originalUuid;
        private BigDecimal shareRatio;
        private BigDecimal consumeRatio;
    }

    @Data
    public static class FinishLayerDTO {
        private Integer outDiameter;
        private Integer coreDiameter;
    }
}

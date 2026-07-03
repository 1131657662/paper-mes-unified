package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RewindLayoutItemPlanDTO {

    @NotNull(message = "门幅不能为空")
    @Min(value = 1, message = "门幅必须大于0")
    private Integer width;

    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    /** FINISH成品，TRIM修边。 */
    private String itemType;

    @Valid
    private List<FinishConfigSpecDTO.FinishLayerDTO> layers;
}

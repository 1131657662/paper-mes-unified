package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RewindLayoutItemPlanDTO {

    @NotNull(message = "门幅不能为空")
    @Min(value = 1, message = "门幅必须大于0")
    private Integer width;

    @Min(value = 1, message = "数量至少为1")
    @Max(value = 500, message = "单个排版数量不能超过500")
    private Integer quantity;

    /** FINISH成品，TRIM修边。 */
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

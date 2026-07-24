package com.paper.mes.machine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class MachineCapabilitySaveDTO {

    @NotBlank(message = "工艺能力不能为空")
    private String catalogUuid;
    @Min(value = 0, message = "默认标记无效")
    @Max(value = 1, message = "默认标记无效")
    private Integer isDefault;
    @Min(value = 1, message = "候选优先级不能小于1")
    @Max(value = 9999, message = "候选优先级不能大于9999")
    private Integer priority;
    @Min(value = 1, message = "最小门幅必须大于0")
    private Integer minWidth;
    @Min(value = 1, message = "最大门幅必须大于0")
    private Integer maxWidth;
    @DecimalMin(value = "0.001", message = "最大卷重必须大于0")
    private BigDecimal maxRollWeight;
    @Min(value = 1, message = "最大卷径必须大于0")
    private Integer maxDiameter;
    @Size(max = 255, message = "能力备注不能超过255个字符")
    private String remark;
}

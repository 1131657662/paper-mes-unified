package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RewindSegmentPlanDTO {

    private Integer segmentSort;
    private BigDecimal segmentRatio;
    private Integer targetDiameter;
    private Integer finishCoreDiameter;
    @Min(value = 1, message = "分段重复次数至少为1")
    @Max(value = 500, message = "分段重复次数不能超过500")
    private Integer repeatCount;

    @Valid
    @Size(max = 100, message = "复卷来源不能超过100条")
    private List<RewindSourcePlanDTO> sources;

    @Valid
    @Size(max = 500, message = "复卷排版项不能超过500条")
    private List<RewindLayoutItemPlanDTO> layoutItems;
}

package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProcessPlanDTO {

    @NotNull(message = "加工模式不能为空")
    private Integer processMode;

    private Integer mainStepType;
    private String machineUuid;

    @Min(value = 0, message = "备用号数量不能小于0")
    @Max(value = 500, message = "备用号数量不能超过500")
    private Integer spareCount;

    private Integer rewindMode;
    private Integer knifeCount;
    private BigDecimal unitPrice;
    @Pattern(regexp = "(?i)LOSS|ALLOCATE|REMAINDER",
            message = "门幅差额处理只能选择LOSS、ALLOCATE或REMAINDER")
    private String widthDifferencePolicy;
    private String remark;

    @Valid
    @Size(max = 500, message = "成品规格不能超过500条")
    private List<FinishConfigSpecDTO> finishSpecs;

    @Valid
    @Size(max = 100, message = "复卷段不能超过100条")
    private List<RewindSegmentPlanDTO> segments;
}

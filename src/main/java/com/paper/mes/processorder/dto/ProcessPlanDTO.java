package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
    private Integer spareCount;

    private Integer rewindMode;
    private Integer knifeCount;
    private BigDecimal unitPrice;
    private String remark;

    @Valid
    private List<FinishConfigSpecDTO> finishSpecs;

    @Valid
    private List<RewindSegmentPlanDTO> segments;
}

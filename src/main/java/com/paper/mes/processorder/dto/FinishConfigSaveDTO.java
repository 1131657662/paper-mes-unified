package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单卷成品配置保存入参。
 */
@Data
public class FinishConfigSaveDTO {

    @NotNull(message = "加工模式不能为空")
    private Integer processMode;

    private Integer mainStepType;
    private String machineUuid;

    @Min(value = 0, message = "备用号数量不能小于 0")
    private Integer spareCount;

    private Integer rewindMode;
    private Integer knifeCount;
    private BigDecimal unitPrice;

    @Valid
    @Size(max = 500, message = "成品规格不能超过500条")
    private List<FinishConfigSpecDTO> finishSpecs;

    @Valid
    @Size(max = 100, message = "复卷段不能超过100条")
    private List<RewindPlanPreviewDTO.RewindSegmentDTO> rewindSegments;
}

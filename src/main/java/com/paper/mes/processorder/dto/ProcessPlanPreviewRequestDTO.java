package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProcessPlanPreviewRequestDTO {

    @NotBlank(message = "原纸UUID不能为空")
    private String originalUuid;

    @Valid
    @NotNull(message = "加工方案不能为空")
    private ProcessPlanDTO plan;
}

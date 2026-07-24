package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProcessPlanPreviewRequestDTO {

    @jakarta.validation.constraints.NotNull(message = "草稿版本不能为空")
    @jakarta.validation.constraints.Min(value = 0, message = "草稿版本不能小于0")
    private Integer expectedVersion;

    @NotBlank(message = "原纸UUID不能为空")
    private String originalUuid;

    @Valid
    @NotNull(message = "加工方案不能为空")
    private ProcessPlanDTO plan;
}

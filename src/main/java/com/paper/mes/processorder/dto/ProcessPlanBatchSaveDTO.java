package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ProcessPlanBatchSaveDTO {

    @NotEmpty(message = "请选择需要应用的原纸")
    private List<String> originalUuids;

    @Valid
    @NotNull(message = "加工方案不能为空")
    private ProcessPlanDTO plan;
}

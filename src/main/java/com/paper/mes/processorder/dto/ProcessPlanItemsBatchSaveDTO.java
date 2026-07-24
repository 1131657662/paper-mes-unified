package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProcessPlanItemsBatchSaveDTO {

    @NotNull(message = "草稿版本不能为空")
    @Min(value = 0, message = "草稿版本不能小于0")
    private Integer expectedVersion;

    @Valid
    @NotEmpty(message = "请选择需要应用的原纸")
    @Size(max = 500, message = "单次应用原纸不能超过500条")
    private List<ProcessPlanBatchItemDTO> items;
}

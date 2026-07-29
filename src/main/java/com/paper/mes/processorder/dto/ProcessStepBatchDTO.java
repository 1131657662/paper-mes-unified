package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProcessStepBatchDTO {

    @Min(value = 0, message = "加工单版本不能小于0")
    private Integer expectedVersion;

    @NotEmpty(message = "至少选择一卷母卷")
    @Size(max = 500, message = "单次最多配置500卷母卷")
    @Valid
    private List<ProcessStepDTO> steps;
}

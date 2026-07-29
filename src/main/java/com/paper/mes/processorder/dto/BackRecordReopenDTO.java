package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BackRecordReopenDTO {

    @NotNull(message = "加工单版本不能为空")
    @Min(value = 0, message = "加工单版本不能小于0")
    private Integer expectedVersion;

    @NotEmpty(message = "请选择要撤回的已回录母卷")
    @Size(max = 100, message = "单次最多撤回100卷母卷")
    private List<String> rollUuids;
}

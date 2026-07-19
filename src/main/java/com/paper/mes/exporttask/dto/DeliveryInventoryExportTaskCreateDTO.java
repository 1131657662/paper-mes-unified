package com.paper.mes.exporttask.dto;

import com.paper.mes.delivery.dto.DeliveryInventoryFinishQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeliveryInventoryExportTaskCreateDTO {
    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号不能超过64个字符")
    private String requestId;

    @Valid
    @NotNull(message = "导出筛选条件不能为空")
    private DeliveryInventoryFinishQuery query;
}

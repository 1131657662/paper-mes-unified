package com.paper.mes.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DeliveryInventoryAvailabilityRequest {

    @NotBlank(message = "客户不能为空")
    @Size(max = 64, message = "客户标识不能超过64个字符")
    private String customerUuid;

    @NotBlank(message = "出库仓库不能为空")
    @Size(max = 64, message = "仓库标识不能超过64个字符")
    private String warehouseUuid;

    @NotEmpty(message = "请选择需要校验的成品卷")
    @Size(max = 500, message = "单次最多校验500卷")
    private List<@NotBlank(message = "成品卷标识不能为空")
            @Size(max = 64, message = "成品卷标识不能超过64个字符") String> finishUuids;
}

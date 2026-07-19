package com.paper.mes.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DeliveryInventoryWarehouseRepairRequest {

    @NotEmpty(message = "请选择需要补仓的加工单")
    @Size(max = 50, message = "单次最多处理50个加工单")
    private List<@NotBlank(message = "加工单标识不能为空")
            @Size(max = 64, message = "加工单标识不能超过64个字符") String> orderUuids;

    @NotBlank(message = "请选择补录仓库")
    @Size(max = 64, message = "仓库标识不能超过64个字符")
    private String warehouseUuid;

    @NotBlank(message = "请填写补仓原因")
    @Size(min = 4, max = 200, message = "补仓原因需为4至200个字符")
    private String reason;
}

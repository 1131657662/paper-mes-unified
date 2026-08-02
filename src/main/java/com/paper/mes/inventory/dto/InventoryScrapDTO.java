package com.paper.mes.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class InventoryScrapDTO {

    @NotBlank(message = "报废原因不能为空")
    @Size(max = 500, message = "报废原因不能超过500个字符")
    private String reason;

    @NotBlank(message = "报废请求号不能为空")
    @Size(max = 36, message = "报废请求号不能超过36个字符")
    private String requestUuid;
}

package com.paper.mes.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryOpeningRequest {

    @NotBlank(message = "切换批次号不能为空")
    @Size(max = 36, message = "切换批次号不能超过36个字符")
    private String switchUuid;

    @NotNull(message = "切换时间不能为空")
    private LocalDateTime occurredAt;
}

package com.paper.mes.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 出库签收回退参数。
 */
@Data
public class DeliveryRollbackDTO {

    @NotBlank(message = "回退原因不能为空")
    @Size(max = 255, message = "回退原因不能超过255个字符")
    private String reason;
}

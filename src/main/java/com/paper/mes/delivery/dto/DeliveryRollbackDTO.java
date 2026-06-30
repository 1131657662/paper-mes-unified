package com.paper.mes.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 出库签收回退参数。
 */
@Data
public class DeliveryRollbackDTO {

    @NotBlank(message = "回退原因不能为空")
    private String reason;
}

package com.paper.mes.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Pending delivery cancellation parameters. */
@Data
public class DeliveryCancelDTO {

    @NotBlank(message = "作废原因不能为空")
    @Size(max = 255, message = "作废原因不能超过255个字符")
    private String reason;
}

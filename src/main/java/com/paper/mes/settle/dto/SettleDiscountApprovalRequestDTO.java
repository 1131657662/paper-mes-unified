package com.paper.mes.settle.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettleDiscountApprovalRequestDTO {
    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号不能超过64个字符")
    private String requestId;

    @NotNull(message = "优惠金额不能为空")
    @DecimalMin(value = "0.01", message = "优惠金额必须大于0")
    private BigDecimal discountAmount;

    @NotBlank(message = "优惠原因不能为空")
    @Size(max = 255, message = "优惠原因不能超过255个字符")
    private String reason;
}

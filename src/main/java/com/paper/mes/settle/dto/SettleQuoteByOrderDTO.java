package com.paper.mes.settle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SettleQuoteByOrderDTO {
    @NotBlank(message = "加工单不能为空")
    private String orderUuid;

    @Min(value = 1, message = "开票状态不正确")
    @Max(value = 2, message = "开票状态不正确")
    private Integer isInvoice;
}

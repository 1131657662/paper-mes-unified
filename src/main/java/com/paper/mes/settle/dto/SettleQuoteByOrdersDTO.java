package com.paper.mes.settle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SettleQuoteByOrdersDTO {
    @NotEmpty(message = "加工单不能为空")
    @Size(max = 500, message = "单次结算加工单不能超过500条")
    private List<@NotBlank(message = "加工单uuid不能为空") String> orderUuids;

    @Min(value = 1, message = "开票状态不正确")
    @Max(value = 2, message = "开票状态不正确")
    private Integer isInvoice;
}

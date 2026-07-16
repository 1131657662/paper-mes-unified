package com.paper.mes.settle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 按单生成结算单入参。
 */
@Data
public class SettleByOrderDTO {

    @NotBlank(message = "加工单不能为空")
    private String orderUuid;

    /** 结算日期，可空默认今天 */
    private LocalDate settleDate;

    /** 是否开票 1开票 2不开票，可空则取加工单 isInvoice */
    @Min(value = 1, message = "开票状态不正确")
    @Max(value = 2, message = "开票状态不正确")
    private Integer isInvoice;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}

package com.paper.mes.settle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 按月批量生成结算单入参。自动圈出该客户该期间内 已完成(4) 且未结算的全部加工单。
 */
@Data
public class SettleByMonthDTO {

    @NotBlank(message = "客户不能为空")
    private String customerUuid;

    @NotNull(message = "账期开始日不能为空")
    private LocalDate periodStart;

    @NotNull(message = "账期结束日不能为空")
    private LocalDate periodEnd;

    /** 结算日期，可空默认今天 */
    private LocalDate settleDate;

    /** 是否开票 1开票 2不开票，可空则取客户缺省 */
    @Min(value = 1, message = "开票状态不正确")
    @Max(value = 2, message = "开票状态不正确")
    private Integer isInvoice;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}

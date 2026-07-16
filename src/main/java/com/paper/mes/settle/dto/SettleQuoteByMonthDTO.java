package com.paper.mes.settle.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SettleQuoteByMonthDTO {
    @NotBlank(message = "客户不能为空")
    private String customerUuid;

    @NotNull(message = "账期开始日不能为空")
    private LocalDate periodStart;

    @NotNull(message = "账期结束日不能为空")
    private LocalDate periodEnd;

    @Min(value = 1, message = "开票状态不正确")
    @Max(value = 2, message = "开票状态不正确")
    private Integer isInvoice;

    @AssertTrue(message = "账期开始日不能晚于结束日")
    public boolean isPeriodOrdered() {
        return periodStart == null || periodEnd == null || !periodStart.isAfter(periodEnd);
    }
}

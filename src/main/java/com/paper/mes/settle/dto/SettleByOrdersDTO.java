package com.paper.mes.settle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 勾选多张已完成加工单生成结算单；单张时等价于按单生成，多张时按合并结算入账。
 */
@Data
public class SettleByOrdersDTO {

    @NotEmpty(message = "加工单不能为空")
    @Size(max = 500, message = "单次结算加工单不能超过500条")
    private List<@NotBlank(message = "加工单uuid不能为空") String> orderUuids;

    private LocalDate periodStart;
    private LocalDate periodEnd;
    /** 结算日期，可空默认今天 */
    private LocalDate settleDate;
    /** 是否开票 1开票 2不开票，可空则取加工单/客户默认值 */
    @Min(value = 1, message = "开票状态不正确")
    @Max(value = 2, message = "开票状态不正确")
    private Integer isInvoice;
    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    @AssertTrue(message = "账期开始日和结束日必须同时填写")
    public boolean isPeriodComplete() {
        return (periodStart == null) == (periodEnd == null);
    }

    @AssertTrue(message = "账期开始日不能晚于结束日")
    public boolean isPeriodOrdered() {
        return periodStart == null || periodEnd == null || !periodStart.isAfter(periodEnd);
    }
}

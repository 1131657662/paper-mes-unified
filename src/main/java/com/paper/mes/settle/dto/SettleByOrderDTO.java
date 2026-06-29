package com.paper.mes.settle.dto;

import jakarta.validation.constraints.NotBlank;
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
    private Integer isInvoice;

    private String remark;
}

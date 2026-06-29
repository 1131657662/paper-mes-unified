package com.paper.mes.settle.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 勾选多张已完成加工单生成结算单；单张时等价于按单生成，多张时按合并结算入账。
 */
@Data
public class SettleByOrdersDTO {

    @NotEmpty(message = "加工单不能为空")
    private List<String> orderUuids;

    private LocalDate periodStart;
    private LocalDate periodEnd;
    /** 结算日期，可空默认今天 */
    private LocalDate settleDate;
    /** 是否开票 1开票 2不开票，可空则取加工单/客户默认值 */
    private Integer isInvoice;
    private String remark;
}

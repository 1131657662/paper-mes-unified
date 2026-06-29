package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 融合版新建加工单向导的基础信息草稿。
 */
@Data
public class DraftOrderBaseDTO {

    @NotNull(message = "客户不能为空")
    private String customerUuid;

    @NotNull(message = "制单日期不能为空")
    private LocalDate orderDate;

    private LocalDate expectFinishDate;
    private Integer priority;
    private String labelBrand;
    private String warehouseUuid;
    private String teamGroup;
    private Integer isInvoice;
    private Integer settleType;
    private Integer settleDay;
    private BigDecimal taxRate;
    private BigDecimal urgentFee;
    private BigDecimal palletFee;
    private BigDecimal loadingFee;
    private BigDecimal freightFee;
    private BigDecimal otherFee;
    private String remark;
    private String remarkLong;
}

package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 加工单创建入参（主表 + 原纸明细列表）。
 */
@Data
public class ProcessOrderCreateDTO {

    @NotNull(message = "客户不能为空")
    private String customerUuid;

    @NotNull(message = "制单日期不能为空")
    private LocalDate orderDate;

    private LocalDate expectFinishDate;
    /** 1普通 2加急 3特急 */
    private Integer priority;
    private String labelBrand;
    private String warehouseUuid;
    private String teamGroup;

    /** 1开票 2不开票 */
    private Integer isInvoice;
    /** 1次结 2月结，可空则取客户档案默认值 */
    private Integer settleType;
    /** 月结对账日，可空则取客户档案默认值 */
    private Integer settleDay;
    private BigDecimal taxRate;
    private BigDecimal urgentFee;
    private BigDecimal palletFee;
    private BigDecimal loadingFee;
    private BigDecimal freightFee;
    private BigDecimal otherFee;

    private String remark;
    private String remarkLong;

    @NotEmpty(message = "原纸明细不能为空")
    @Valid
    private List<OriginalRollDTO> originalRolls;
}

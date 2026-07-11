package com.paper.mes.processorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 加工单创建入参。
 */
@Data
public class ProcessOrderCreateDTO {

    @NotNull(message = "客户不能为空")
    private String customerUuid;

    @NotNull(message = "制单日期不能为空")
    private LocalDate orderDate;

    private LocalDate expectFinishDate;
    /** 1普通 2加急 3特急 */
    @Min(value = 1, message = "优先级不正确")
    @Max(value = 3, message = "优先级不正确")
    private Integer priority;
    private String labelBrand;
    private String warehouseUuid;
    private String teamGroup;

    /** 1开票 2不开票 */
    @Min(value = 1, message = "开票状态不正确")
    @Max(value = 2, message = "开票状态不正确")
    private Integer isInvoice;
    /** 1次结 2月结，可空则取客户档案默认值 */
    @Min(value = 1, message = "结算方式不正确")
    @Max(value = 2, message = "结算方式不正确")
    private Integer settleType;
    /** 月结对账日，可空则取客户档案默认值 */
    @Min(value = 1, message = "月结日必须在1-31之间")
    @Max(value = 31, message = "月结日必须在1-31之间")
    private Integer settleDay;
    @DecimalMin(value = "0.00", message = "税率不能为负")
    @DecimalMax(value = "100.00", message = "税率不能超过100%")
    private BigDecimal taxRate;
    @PositiveOrZero(message = "加急费不能为负")
    private BigDecimal urgentFee;
    @PositiveOrZero(message = "托盘费不能为负")
    private BigDecimal palletFee;
    @PositiveOrZero(message = "装车费不能为负")
    private BigDecimal loadingFee;
    @PositiveOrZero(message = "运费不能为负")
    private BigDecimal freightFee;
    @PositiveOrZero(message = "其他费用不能为负")
    private BigDecimal otherFee;

    private String remark;
    private String remarkLong;

    @NotEmpty(message = "原纸明细不能为空")
    @Valid
    private List<OriginalRollDTO> originalRolls;
}

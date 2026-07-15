package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 融合版新建加工单向导的基础信息草稿。
 */
@Data
public class DraftOrderBaseDTO {

    @NotBlank(message = "客户不能为空")
    private String customerUuid;

    @NotNull(message = "制单日期不能为空")
    private LocalDate orderDate;

    private LocalDate expectFinishDate;
    @Min(value = 1, message = "优先级不正确")
    @Max(value = 3, message = "优先级不正确")
    private Integer priority;
    @Size(max = 100, message = "标签品牌长度不能超过100")
    private String labelBrand;
    @Size(max = 32, message = "仓库标识长度不能超过32")
    private String warehouseUuid;
    @Size(max = 50, message = "班组长度不能超过50")
    private String teamGroup;
    @Min(value = 1, message = "开票状态不正确")
    @Max(value = 2, message = "开票状态不正确")
    private Integer isInvoice;
    @Min(value = 1, message = "结算方式不正确")
    @Max(value = 2, message = "结算方式不正确")
    private Integer settleType;
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
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
    @Size(max = 2000, message = "详细备注长度不能超过2000")
    private String remarkLong;
}

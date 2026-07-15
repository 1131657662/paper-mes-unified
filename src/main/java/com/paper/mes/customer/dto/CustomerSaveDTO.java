package com.paper.mes.customer.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Customer create/update payload.
 */
@Data
public class CustomerSaveDTO {

    @Size(max = 50, message = "\u5ba2\u6237\u7f16\u7801\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc750")
    private String customerCode;

    @NotBlank(message = "\u5ba2\u6237\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a")
    @Size(max = 100, message = "\u5ba2\u6237\u540d\u79f0\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7100")
    private String customerName;

    @Size(max = 50, message = "联系人长度不能超过50")
    private String contact;
    @Size(max = 50, message = "联系电话长度不能超过50")
    private String phone;
    @Min(value = 1, message = "结算方式不正确")
    @Max(value = 2, message = "结算方式不正确")
    private Integer settleType;
    @Min(value = 1, message = "月结日必须在1-31之间")
    @Max(value = 31, message = "月结日必须在1-31之间")
    private Integer settleDay;
    @PositiveOrZero(message = "锯纸单价不能为负")
    private BigDecimal sawPrice;
    @PositiveOrZero(message = "复卷单价不能为负")
    private BigDecimal rewindPrice;
    @Min(value = 1, message = "开票状态不正确")
    @Max(value = 2, message = "开票状态不正确")
    private Integer defaultInvoice;
    @Min(value = 1, message = "价格口径不正确")
    @Max(value = 2, message = "价格口径不正确")
    private Integer priceIncludeTax;
    @DecimalMin(value = "0.00", message = "税率不能为负")
    @DecimalMax(value = "100.00", message = "税率不能超过100%")
    private BigDecimal taxRate;
    @Size(max = 50, message = "税号长度不能超过50")
    private String taxNo;
    @Size(max = 255, message = "开票地址电话长度不能超过255")
    private String invoiceAddress;
    @Size(max = 100, message = "开户行账号长度不能超过100")
    private String bankAccount;
    @Size(max = 255, message = "送货地址长度不能超过255")
    private String deliveryAddress;
    @Min(value = 1, message = "客户等级不正确")
    @Max(value = 3, message = "客户等级不正确")
    private Integer customerLevel;
    @Size(max = 50, message = "模板标识长度不能超过50")
    private String exportTemplate;
    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}

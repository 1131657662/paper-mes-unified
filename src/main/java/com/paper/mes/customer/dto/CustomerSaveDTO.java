package com.paper.mes.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户新增/修改入参。
 */
@Data
public class CustomerSaveDTO {

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 50, message = "客户编码长度不能超过50")
    private String customerCode;

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称长度不能超过100")
    private String customerName;

    private String contact;
    private String phone;
    private Integer settleType;
    private Integer settleDay;
    private BigDecimal sawPrice;
    private BigDecimal rewindPrice;
    private Integer defaultInvoice;
    private Integer priceIncludeTax;
    private BigDecimal taxRate;
    private String taxNo;
    private String invoiceAddress;
    private String bankAccount;
    private String deliveryAddress;
    private Integer customerLevel;
    private String exportTemplate;
    private String remark;
}

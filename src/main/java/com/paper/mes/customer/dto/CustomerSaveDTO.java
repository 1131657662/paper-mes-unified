package com.paper.mes.customer.dto;

import jakarta.validation.constraints.NotBlank;
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

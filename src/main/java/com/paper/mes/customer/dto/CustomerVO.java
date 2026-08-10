package com.paper.mes.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerVO {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String uuid;
    private Integer version;
    private String customerCode;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerName;
    private String contact;
    private String phone;
    @Schema(allowableValues = {"1", "2"})
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<CustomerProcessPriceVO> processPrices;
}

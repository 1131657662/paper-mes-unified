package com.paper.mes.settle.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算费用来源行：用于详情、打印和导出解释每笔加工费的计算口径。
 */
@Data
public class SettleFeeLineVO {

    /** saw / rewind / service / extra / tax */
    private String feeType;
    private String feeName;
    private Integer stageLevel;
    private String sourceText;
    private String outputText;
    private BigDecimal quantity;
    private String quantityUnit;
    private BigDecimal unitPrice;
    private BigDecimal standardQuantity;
    private BigDecimal standardAmount;
    private Integer billingMode;
    private BigDecimal pricingAdjustmentAmount;
    private String pricingAdjustmentReason;
    private BigDecimal amountNoTax;
    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private BigDecimal amountTax;
    private String formulaText;
    private String remark;
}

package com.paper.mes.settle.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 结算单打印/导出明细行：按加工单下的原纸卷展示加工、成品与费用。
 */
@Data
public class SettlePrintLineVO {

    private String settleUuid;
    private String orderUuid;
    private String orderNo;
    private LocalDate orderDate;
    private String originalUuid;
    private String originalLabel;
    private String originalRollNo;
    private String originalExtraNo;
    private String paperName;
    private Integer gramWeight;
    private Integer actualGramWeight;
    private Integer originalWidth;
    private Integer actualWidth;
    private Integer originalDiameter;
    private Integer coreDiameter;
    private Integer originalLength;
    private BigDecimal originalWeight;
    private Integer processMode;
    private Integer mainStepType;
    private String machineUuid;
    private String machineName;
    private String processText;
    private String processStepSummary;
    private String finishSummary;
    private String finishDetailSummary;
    private Integer finishCount;
    private BigDecimal finishWeight;
    private BigDecimal trimWeight;
    private String trimSummary;
    private BigDecimal sawWeight;
    private BigDecimal rewindWeight;
    private BigDecimal sawUnitPrice;
    /** 开票时按税点换算后的锯纸展示单价；不开票时等于未税单价。 */
    private BigDecimal sawInvoiceUnitPrice;
    private BigDecimal rewindUnitPrice;
    /** 开票时按税点换算后的复卷展示单价；不开票时等于未税单价。 */
    private BigDecimal rewindInvoiceUnitPrice;
    private BigDecimal sawAmount;
    private BigDecimal rewindAmount;
    /** 锯纸费 + 复卷费。 */
    private BigDecimal processAmount;
    /** 分摊到本原纸行的额外费用。 */
    private BigDecimal extraAmount;
    /** 额外费用构成说明，例如：装卸费 80.00、运费 30.00。 */
    private String extraFeeSummary;
    /** 分摊到本原纸行的开票加价；不开票时为 0。 */
    private BigDecimal taxAmount;
    private BigDecimal taxRate;
    /** 本行应收合计，所有行汇总应等于结算单应收总额。 */
    private BigDecimal lineAmount;
    private Integer isInvoice;
    private String remark;
}

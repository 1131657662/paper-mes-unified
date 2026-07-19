package com.paper.mes.processorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 整单计费结果（P1-5）。含整单各金额、逐卷加工费、逐工序费用明细，供前端展示与对账。
 */
@Data
public class FeeResultVO {

    private String orderUuid;
    private String orderNo;

    private BigDecimal totalProcessAmount;
    private BigDecimal totalExtraAmount;
    private BigDecimal totalAmountNoTax;
    private BigDecimal totalAmountTax;
    private BigDecimal totalAmount;
    private Integer actualTotalKnife;
    /** 0单一工艺 1混合锯纸+复卷 */
    private Integer isMixProcess;

    private List<RollFee> rollFees;
    private List<StepFee> stepFees;

    @Data
    public static class RollFee {
        private String originalUuid;
        private String rollNo;
        private BigDecimal processAmount;
    }

    @Data
    public static class StepFee {
        private String stepUuid;
        private String originalUuid;
        /** 1锯纸 2复卷 */
        private Integer stepType;
        /** 标准单价快照。 */
        private BigDecimal unitPrice;
        private BigDecimal billingUnitPrice;
        private BigDecimal effectiveUnitPrice;
        /** 计费数量：锯纸=刀数，复卷=吨位 */
        private BigDecimal quantity;
        private BigDecimal standardQuantity;
        private BigDecimal standardStepAmount;
        private Integer billingMode;
        private BigDecimal pricingAdjustmentAmount;
        private BigDecimal stepAmount;
    }
}

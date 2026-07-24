package com.paper.mes.settle.service.impl;

import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.service.ProcessStepPricingPolicy;
import com.paper.mes.settle.dto.SettleFeeLineVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

final class SettleServiceFeeLineBuilder {

    private static final int STEP_TYPE_STRIP_SORT = 3;

    private SettleServiceFeeLineBuilder() {
    }

    static SettleFeeLineVO build(SettlePrintLineVO line, ProcessStep step) {
        SettleFeeLineVO feeLine = SettleFeeLineSupport.baseLine("service", name(step), line);
        BigDecimal quantity = step.getBillingQuantity() == null
                ? step.getServiceQuantity() : step.getBillingQuantity();
        String unit = "PIECE".equals(step.getBillingBasis()) ? "件" : "t";
        feeLine.setStageLevel(step.getStageLevel());
        feeLine.setQuantity(quantity);
        feeLine.setQuantityUnit(unit);
        feeLine.setUnitPrice(effectivePrice(step));
        feeLine.setStandardQuantity(step.getStandardQuantity() == null ? quantity : step.getStandardQuantity());
        feeLine.setStandardAmount(SettleFeeLineSupport.money(step.getStandardStepAmount()));
        feeLine.setBillingMode(step.getBillingMode() == null ? ProcessStepPricingPolicy.STANDARD : step.getBillingMode());
        feeLine.setPricingAdjustmentAmount(SettleFeeLineSupport.money(step.getPricingAdjustmentAmount()));
        feeLine.setPricingAdjustmentReason(step.getPricingAdjustmentReason());
        feeLine.setAmountNoTax(SettleFeeLineSupport.money(step.getStepAmount()));
        feeLine.setAmountTax(SettleFeeLineSupport.money(step.getStepAmount()));
        feeLine.setFormulaText(formula(step, quantity, unit));
        feeLine.setRemark(step.getRemark());
        return feeLine;
    }

    private static String name(ProcessStep step) {
        if (StringUtils.hasText(step.getStepName())) return step.getStepName();
        return step.getStepType() == STEP_TYPE_STRIP_SORT ? "剥损整理" : "重新包装";
    }

    private static BigDecimal effectivePrice(ProcessStep step) {
        return step.getBillingUnitPrice() == null ? step.getUnitPrice() : step.getBillingUnitPrice();
    }

    private static String formula(ProcessStep step, BigDecimal quantity, String unit) {
        String base = quantity == null ? "未填写服务数量"
                : quantity.stripTrailingZeros().toPlainString() + unit + " × "
                + SettleFeeLineSupport.moneyText(effectivePrice(step)) + "元/" + unit;
        return SettleFeeLineSupport.pricingFormula(step, base, unit);
    }
}

package com.paper.mes.settle.service.impl;

import com.paper.mes.settle.dto.SettleFeeLineVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

final class SettleFeeLineFallbackBuilder {

    private static final int QUANTITY_SCALE = 3;

    private SettleFeeLineFallbackBuilder() {
    }

    static void ensure(List<SettlePrintLineVO> lines) {
        if (lines == null) {
            return;
        }
        for (SettlePrintLineVO line : lines) {
            if (line.getFeeLines() == null || line.getFeeLines().isEmpty()) {
                line.setFeeLines(fallbackLines(line));
            }
        }
    }

    private static List<SettleFeeLineVO> fallbackLines(SettlePrintLineVO line) {
        List<SettleFeeLineVO> result = new ArrayList<>();
        appendProcessLine(result, line, "saw", "锯纸费", line.getSawAmount(), line.getSawUnitPrice(), "刀");
        appendProcessLine(result, line, "rewind", "复卷费", line.getRewindAmount(), line.getRewindUnitPrice(), "t");
        appendExtraLine(result, line);
        appendTaxLine(result, line);
        return result;
    }

    private static void appendProcessLine(List<SettleFeeLineVO> result, SettlePrintLineVO line, String type,
                                          String name, BigDecimal amount, BigDecimal unitPrice, String unit) {
        if (SettleFeeLineSupport.isZero(amount)) {
            return;
        }
        SettleFeeLineVO feeLine = SettleFeeLineSupport.baseLine(type, name, line);
        feeLine.setQuantity(estimateQuantity(amount, unitPrice));
        feeLine.setQuantityUnit(unit);
        feeLine.setUnitPrice(unitPrice);
        feeLine.setAmountNoTax(SettleFeeLineSupport.money(amount));
        feeLine.setAmountTax(SettleFeeLineSupport.money(amount));
        feeLine.setFormulaText(fallbackFormula(feeLine.getQuantity(), unit, unitPrice, amount));
        result.add(feeLine);
    }

    private static void appendExtraLine(List<SettleFeeLineVO> result, SettlePrintLineVO line) {
        if (SettleFeeLineSupport.isZero(line.getExtraAmount())) {
            return;
        }
        SettleFeeLineVO feeLine = SettleFeeLineSupport.baseLine("extra", "额外费用", line);
        feeLine.setAmountNoTax(SettleFeeLineSupport.money(line.getExtraAmount()));
        feeLine.setAmountTax(SettleFeeLineSupport.money(line.getExtraAmount()));
        feeLine.setFormulaText(StringUtils.hasText(line.getExtraFeeSummary()) ? line.getExtraFeeSummary() : "额外费用");
        result.add(feeLine);
    }

    private static void appendTaxLine(List<SettleFeeLineVO> result, SettlePrintLineVO line) {
        if (SettleFeeLineSupport.isZero(line.getTaxAmount())) {
            return;
        }
        SettleFeeLineVO feeLine = SettleFeeLineSupport.baseLine("tax", "开票加价", line);
        feeLine.setTaxRate(line.getTaxRate());
        feeLine.setTaxAmount(SettleFeeLineSupport.money(line.getTaxAmount()));
        feeLine.setAmountNoTax(SettleFeeLineSupport.zeroMoney());
        feeLine.setAmountTax(SettleFeeLineSupport.money(line.getTaxAmount()));
        feeLine.setFormulaText("按税点 " + SettleFeeLineSupport.percentText(line.getTaxRate()) + " 分摊开票加价");
        result.add(feeLine);
    }

    private static BigDecimal estimateQuantity(BigDecimal amount, BigDecimal unitPrice) {
        if (SettleFeeLineSupport.isZero(amount) || SettleFeeLineSupport.isZero(unitPrice)) {
            return null;
        }
        return amount.divide(unitPrice, QUANTITY_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static String fallbackFormula(BigDecimal quantity, String unit, BigDecimal unitPrice, BigDecimal amount) {
        if (quantity == null || unitPrice == null) {
            return "金额 " + SettleFeeLineSupport.moneyText(amount);
        }
        return quantity.stripTrailingZeros().toPlainString() + unit + " × " + SettleFeeLineSupport.moneyText(unitPrice);
    }
}

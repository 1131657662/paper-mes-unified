package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettleFeeLineVO;
import com.paper.mes.settle.dto.SettlePrintLineVO;

import java.math.BigDecimal;
import java.util.List;

/** Keeps tax display and export consistent with the settlement invoice flag. */
public final class SettlePrintLineTaxPolicy {

    private SettlePrintLineTaxPolicy() {
    }

    public static BigDecimal amount(SettlePrintLineVO line) {
        if (!isInvoice(line)) {
            return BigDecimal.ZERO;
        }
        if (positive(line.getTaxAmount())) {
            return line.getTaxAmount();
        }
        BigDecimal feeTax = feeTax(line.getFeeLines());
        if (positive(feeTax)) {
            return feeTax;
        }
        return nonNegative(nz(line.getLineAmount())
                .subtract(nz(line.getProcessAmount()))
                .subtract(nz(line.getExtraAmount())));
    }

    public static void normalizeNonInvoice(SettlePrintLineVO line) {
        if (isInvoice(line)) {
            return;
        }
        BigDecimal difference = nz(line.getLineAmount())
                .subtract(nz(line.getProcessAmount()))
                .subtract(nz(line.getExtraAmount()));
        if (difference.signum() == 0 && nz(line.getTaxAmount()).signum() != 0) {
            difference = nz(line.getTaxAmount());
        }
        line.setHistoricalDifferenceAmount(difference);
        line.setTaxAmount(BigDecimal.ZERO);
        line.setTaxRate(BigDecimal.ZERO);
        List<SettleFeeLineVO> feeLines = line.getFeeLines();
        if (feeLines != null) {
            line.setFeeLines(feeLines.stream()
                    .filter(fee -> !"tax".equalsIgnoreCase(fee.getFeeType()))
                    .toList());
        }
    }

    private static BigDecimal feeTax(List<SettleFeeLineVO> feeLines) {
        if (feeLines == null) {
            return BigDecimal.ZERO;
        }
        return feeLines.stream()
                .filter(fee -> "tax".equalsIgnoreCase(fee.getFeeType()))
                .map(fee -> fee.getTaxAmount() != null ? fee.getTaxAmount() : nz(fee.getAmountTax()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static boolean isInvoice(SettlePrintLineVO line) {
        return line != null && Integer.valueOf(1).equals(line.getIsInvoice());
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value.max(BigDecimal.ZERO);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

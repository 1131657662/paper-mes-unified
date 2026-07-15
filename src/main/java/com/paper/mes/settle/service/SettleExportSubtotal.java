package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettlePrintLineVO;
import com.paper.mes.settle.dto.SettleFeeLineVO;

import java.math.BigDecimal;

final class SettleExportSubtotal {

    String orderNo;
    int finishCount;
    BigDecimal originalWeight = BigDecimal.ZERO;
    BigDecimal finishWeight = BigDecimal.ZERO;
    BigDecimal trimWeight = BigDecimal.ZERO;
    BigDecimal processAmount = BigDecimal.ZERO;
    BigDecimal extraAmount = BigDecimal.ZERO;
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal lineAmount = BigDecimal.ZERO;
    String extraFeeSummary;

    void add(SettlePrintLineVO line) {
        orderNo = line.getOrderNo();
        finishCount += line.getFinishCount() == null ? 0 : line.getFinishCount();
        originalWeight = originalWeight.add(nz(line.getOriginalWeight()));
        finishWeight = finishWeight.add(nz(line.getFinishWeight()));
        trimWeight = trimWeight.add(nz(line.getTrimWeight()));
        processAmount = processAmount.add(nz(line.getProcessAmount()));
        extraAmount = extraAmount.add(nz(line.getExtraAmount()));
        taxAmount = taxAmount.add(lineTaxAmount(line));
        lineAmount = lineAmount.add(nz(line.getLineAmount()));
        if (extraFeeSummary == null && line.getExtraFeeSummary() != null
                && !line.getExtraFeeSummary().isBlank()) {
            extraFeeSummary = line.getExtraFeeSummary();
        }
    }

    private BigDecimal lineTaxAmount(SettlePrintLineVO line) {
        if (line.getTaxAmount() != null && line.getTaxAmount().signum() > 0) {
            return line.getTaxAmount();
        }
        BigDecimal feeTax = line.getFeeLines() == null ? BigDecimal.ZERO : line.getFeeLines().stream()
                .filter(fee -> "tax".equals(fee.getFeeType()))
                .map(this::feeTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (feeTax.signum() > 0) {
            return feeTax;
        }
        BigDecimal fallback = nz(line.getLineAmount()).subtract(nz(line.getProcessAmount()))
                .subtract(nz(line.getExtraAmount()));
        return fallback.max(BigDecimal.ZERO);
    }

    private BigDecimal feeTaxAmount(SettleFeeLineVO fee) {
        return fee.getTaxAmount() == null ? nz(fee.getAmountTax()) : fee.getTaxAmount();
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

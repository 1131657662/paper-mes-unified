package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettlePrintLineVO;

import java.math.BigDecimal;

final class SettleExportSubtotal {

    String orderNo;
    int originalCount;
    int measuredOriginalCount;
    int estimatedOriginalCount;
    int unknownOriginalCount;
    int unspecifiedOriginalCount;
    int finishCount;
    boolean orderFinishTotalsApplied;
    BigDecimal originalWeight = BigDecimal.ZERO;
    BigDecimal finishWeight = BigDecimal.ZERO;
    BigDecimal trimWeight = BigDecimal.ZERO;
    BigDecimal processAmount = BigDecimal.ZERO;
    BigDecimal extraAmount = BigDecimal.ZERO;
    BigDecimal taxAmount = BigDecimal.ZERO;
    BigDecimal historicalDifferenceAmount = BigDecimal.ZERO;
    BigDecimal lineAmount = BigDecimal.ZERO;
    String extraFeeSummary;

    void add(SettlePrintLineVO line) {
        orderNo = line.getOrderNo();
        addFinishTotals(line);
        addOriginalWeight(line);
        trimWeight = trimWeight.add(nz(line.getTrimWeight()));
        processAmount = processAmount.add(nz(line.getProcessAmount()));
        extraAmount = extraAmount.add(nz(line.getExtraAmount()));
        taxAmount = taxAmount.add(lineTaxAmount(line));
        historicalDifferenceAmount = historicalDifferenceAmount.add(nz(line.getHistoricalDifferenceAmount()));
        lineAmount = lineAmount.add(nz(line.getLineAmount()));
        if (extraFeeSummary == null && line.getExtraFeeSummary() != null
                && !line.getExtraFeeSummary().isBlank()) {
            extraFeeSummary = line.getExtraFeeSummary();
        }
    }

    String originalWeightText() {
        if (originalCount == 0) return "-";
        if (unknownOriginalCount == originalCount) return "重量全部待称重";
        if (unknownOriginalCount > 0) {
            return "已知 " + SettleBillLineText.weightText(originalWeight)
                    + "；" + unknownOriginalCount + " 卷待称重";
        }
        if (estimatedOriginalCount == originalCount) {
            return "参考 " + SettleBillLineText.weightText(originalWeight) + "（未实测）";
        }
        if (measuredOriginalCount == originalCount) {
            return "实测 " + SettleBillLineText.weightText(originalWeight);
        }
        if (unspecifiedOriginalCount == originalCount) {
            return SettleBillLineText.weightText(originalWeight);
        }
        if (unspecifiedOriginalCount > 0) {
            return SettleBillLineText.weightText(originalWeight) + "（状态不完整）";
        }
        return SettleBillLineText.weightText(originalWeight) + "（含未实测）";
    }

    private void addOriginalWeight(SettlePrintLineVO line) {
        originalCount++;
        String status = line.getOriginalWeightStatus();
        if ("UNKNOWN".equalsIgnoreCase(status)) {
            unknownOriginalCount++;
            return;
        }
        originalWeight = originalWeight.add(nz(line.getOriginalWeight()));
        if ("MEASURED".equalsIgnoreCase(status)) measuredOriginalCount++;
        else if ("ESTIMATED".equalsIgnoreCase(status)) estimatedOriginalCount++;
        else unspecifiedOriginalCount++;
    }

    private void addFinishTotals(SettlePrintLineVO line) {
        if (line.getOrderFinishCount() != null || line.getOrderFinishWeight() != null) {
            finishCount = line.getOrderFinishCount() == null ? 0 : line.getOrderFinishCount();
            finishWeight = nz(line.getOrderFinishWeight());
            orderFinishTotalsApplied = true;
            return;
        }
        if (orderFinishTotalsApplied) return;
        finishCount += line.getFinishCount() == null ? 0 : line.getFinishCount();
        finishWeight = finishWeight.add(nz(line.getFinishWeight()));
    }

    private BigDecimal lineTaxAmount(SettlePrintLineVO line) {
        return SettlePrintLineTaxPolicy.amount(line);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

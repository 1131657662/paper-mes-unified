package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.SettlePrintLineVO;

import java.math.BigDecimal;

final class SettleExportSubtotal {

    String orderNo;
    int finishCount;
    BigDecimal originalWeight = BigDecimal.ZERO;
    BigDecimal finishWeight = BigDecimal.ZERO;
    BigDecimal trimWeight = BigDecimal.ZERO;
    BigDecimal processAmount = BigDecimal.ZERO;
    BigDecimal extraAmount = BigDecimal.ZERO;
    BigDecimal lineAmount = BigDecimal.ZERO;

    void add(SettlePrintLineVO line) {
        orderNo = line.getOrderNo();
        finishCount += line.getFinishCount() == null ? 0 : line.getFinishCount();
        originalWeight = originalWeight.add(nz(line.getOriginalWeight()));
        finishWeight = finishWeight.add(nz(line.getFinishWeight()));
        trimWeight = trimWeight.add(nz(line.getTrimWeight()));
        processAmount = processAmount.add(nz(line.getProcessAmount()));
        extraAmount = extraAmount.add(nz(line.getExtraAmount()));
        lineAmount = lineAmount.add(nz(line.getLineAmount()));
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

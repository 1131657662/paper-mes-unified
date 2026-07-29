package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.FinishConfigSpecDTO;
import com.paper.mes.processorder.model.WidthDifferencePolicy;

import java.math.BigDecimal;
import java.util.List;

public record SawPlanCalculation(
        List<CalculatedFinish> finishes,
        List<CalculatedFinish> trims,
        WidthDifferencePolicy policy,
        int sourceWidth,
        int finishWidth,
        int trimWidth,
        int differenceWidth,
        BigDecimal differenceWeight,
        int knifeCount
) {
    public record CalculatedFinish(FinishConfigSpecDTO specification, BigDecimal estimateWeight) {
    }
}

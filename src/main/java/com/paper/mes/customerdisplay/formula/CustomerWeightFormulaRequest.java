package com.paper.mes.customerdisplay.formula;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public record CustomerWeightFormulaRequest(
        String expression,
        Map<String, BigDecimal> variables,
        int roundingScale,
        RoundingMode roundingMode,
        CustomerWeightZeroPolicy zeroPolicy) {
}

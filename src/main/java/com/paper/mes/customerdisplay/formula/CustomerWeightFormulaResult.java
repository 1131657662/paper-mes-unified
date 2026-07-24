package com.paper.mes.customerdisplay.formula;

import java.math.BigDecimal;
import java.util.Set;

public record CustomerWeightFormulaResult(
        Status status,
        BigDecimal rawValue,
        BigDecimal roundedValue,
        Set<String> usedVariables) {

    public enum Status {
        CALCULATED,
        SKIPPED_ZERO
    }
}

package com.paper.mes.customerdisplay.formula;

import java.util.Set;

public final class CustomerWeightFormulaVariables {

    public static final Set<String> ALLOWED = Set.of(
            "physicalWeight",
            "physicalGsm",
            "physicalWidth",
            "customerGsm",
            "customerWidth",
            "sourceWeight",
            "sourceGsm",
            "sourceWidth",
            "finishWeight",
            "finishWidth",
            "lossWeight",
            "adjustment");

    public static final String STANDARD_FORMULA =
            "physicalWeight * (customerGsm / physicalGsm) * (customerWidth / physicalWidth)";

    private CustomerWeightFormulaVariables() {
    }
}

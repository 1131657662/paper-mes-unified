package com.paper.mes.customerdisplay.formula;

import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerWeightFormulaEngineTest {

    private final CustomerWeightFormulaEngine engine = new CustomerWeightFormulaEngine();

    @Test
    void evaluate_standardFormula_convertsGsmWithExactDecimalMath() {
        CustomerWeightFormulaResult result = engine.evaluate(request(
                CustomerWeightFormulaVariables.STANDARD_FORMULA,
                standardInputs("2213", "70", "2000", "75", "2000"),
                CustomerWeightZeroPolicy.ERROR));

        assertEquals(CustomerWeightFormulaResult.Status.CALCULATED, result.status());
        assertEquals(new BigDecimal("2371.071"), result.roundedValue());
    }

    @Test
    void evaluate_widthOverride_appliesCustomerWidthIndependently() {
        CustomerWeightFormulaResult result = engine.evaluate(request(
                CustomerWeightFormulaVariables.STANDARD_FORMULA,
                standardInputs("2213", "70", "502", "75", "500"),
                CustomerWeightZeroPolicy.ERROR));

        assertEquals(new BigDecimal("2361.625"), result.roundedValue());
    }

    @Test
    void evaluate_zeroWithSkipPolicy_returnsSkippedWithoutWritingWeight() {
        CustomerWeightFormulaResult result = engine.evaluate(request(
                "physicalWeight + adjustment",
                Map.of("physicalWeight", BigDecimal.TEN, "adjustment", BigDecimal.ZERO),
                CustomerWeightZeroPolicy.SKIP));

        assertEquals(CustomerWeightFormulaResult.Status.SKIPPED_ZERO, result.status());
        assertNull(result.roundedValue());
    }

    @Test
    void evaluate_missingVariable_isRejected() {
        CustomerWeightFormulaRequest request = request(
                "physicalWeight * customerGsm / physicalGsm",
                Map.of("physicalWeight", BigDecimal.TEN, "customerGsm", BigDecimal.TEN),
                CustomerWeightZeroPolicy.ERROR);

        assertThrows(BusinessException.class, () -> engine.evaluate(request));
    }

    @Test
    void evaluate_unapprovedFunction_isRejected() {
        CustomerWeightFormulaRequest request = request(
                "RANDOM() + physicalWeight",
                Map.of("physicalWeight", BigDecimal.TEN),
                CustomerWeightZeroPolicy.ERROR);

        assertThrows(BusinessException.class, () -> engine.evaluate(request));
    }

    @Test
    void evaluate_divisionByZero_isRejected() {
        CustomerWeightFormulaRequest request = request(
                "physicalWeight / physicalGsm",
                Map.of("physicalWeight", BigDecimal.TEN, "physicalGsm", BigDecimal.ZERO),
                CustomerWeightZeroPolicy.USE_ZERO);

        assertThrows(BusinessException.class, () -> engine.evaluate(request));
    }

    private CustomerWeightFormulaRequest request(
            String expression, Map<String, BigDecimal> inputs, CustomerWeightZeroPolicy zeroPolicy) {
        return new CustomerWeightFormulaRequest(expression, inputs, 3, RoundingMode.HALF_UP, zeroPolicy);
    }

    private Map<String, BigDecimal> standardInputs(
            String weight, String gsm, String width, String customerGsm, String customerWidth) {
        return Map.of(
                "physicalWeight", new BigDecimal(weight),
                "physicalGsm", new BigDecimal(gsm),
                "physicalWidth", new BigDecimal(width),
                "customerGsm", new BigDecimal(customerGsm),
                "customerWidth", new BigDecimal(customerWidth));
    }
}

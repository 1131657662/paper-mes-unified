package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.customerdisplay.formula.CustomerWeightCalculationMode;
import com.paper.mes.customerdisplay.formula.CustomerWeightFormulaEngine;
import com.paper.mes.customerdisplay.formula.CustomerWeightFormulaVariables;
import com.paper.mes.customerdisplay.formula.CustomerWeightZeroPolicy;
import com.paper.mes.processorder.dto.FinishCustomerSpecItemDTO;
import com.paper.mes.processorder.dto.FinishCustomerSpecVO;
import com.paper.mes.processorder.entity.FinishRoll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinishCustomerSpecPlannerTest {

    private final FinishCustomerSpecPlanner planner =
            new FinishCustomerSpecPlanner(new CustomerWeightFormulaEngine());

    @Test
    void current_withoutOverrides_fallsBackToPhysicalFacts() {
        FinishCustomerSpecVO result = planner.current(finish());

        assertEquals("白卡", result.getCustomerPaperName());
        assertEquals(70, result.getCustomerGramWeight());
        assertEquals(502, result.getCustomerFinishWidth());
        assertEquals(new BigDecimal("2213"), result.getCustomerDisplayWeight());
    }

    @Test
    void plan_formula_usesServerPhysicalFactsInsteadOfClientSpoofedValues() {
        FinishCustomerSpecItemDTO item = formulaItem();
        item.setFormulaVariables(Map.of("physicalWeight", new BigDecimal("9999")));

        FinishCustomerSpecVO result = planner.plan(finish(), item);

        assertEquals(new BigDecimal("2361.625"), result.getCustomerDisplayWeight());
        assertTrue(result.isSpecificationChanged());
        assertTrue(result.isWeightChanged());
    }

    @Test
    void plan_spareFinish_isRejected() {
        FinishRoll finish = finish();
        finish.setIsSpare(1);

        assertThrows(BusinessException.class, () -> planner.plan(finish, formulaItem()));
    }

    @Test
    void current_whenPhysicalWeightMissing_returnsValidationStateInsteadOfCrashing() {
        FinishRoll finish = finish();
        finish.setActualWeight(null);

        FinishCustomerSpecVO result = planner.current(finish);

        assertNull(result.getCustomerDisplayWeight());
        assertFalse(result.isValid());
        assertEquals("客户品名、克重、门幅或重量不完整", result.getError());
    }

    @Test
    void plan_formulaWhenPhysicalGsmMissing_reportsBusinessValidationError() {
        FinishRoll finish = finish();
        finish.setGramWeight(null);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> planner.plan(finish, formulaItem()));

        assertTrue(exception.getMessage().contains("缺少参数"));
    }

    private FinishRoll finish() {
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setVersion(1);
        finish.setFinishRollNo("A000001");
        finish.setPaperName("白卡");
        finish.setGramWeight(70);
        finish.setFinishWidth(502);
        finish.setActualWeight(new BigDecimal("2213"));
        finish.setIsSpare(0);
        finish.setIsRemain(0);
        finish.setRollNoStatus(2);
        return finish;
    }

    private FinishCustomerSpecItemDTO formulaItem() {
        FinishCustomerSpecItemDTO item = new FinishCustomerSpecItemDTO();
        item.setFinishUuid("finish-1");
        item.setExpectedVersion(1);
        item.setCustomerPaperName("食品卡");
        item.setCustomerGramWeight(75);
        item.setCustomerFinishWidth(500);
        item.setCalculationMode(CustomerWeightCalculationMode.FORMULA);
        item.setFormulaExpression(CustomerWeightFormulaVariables.STANDARD_FORMULA);
        item.setRoundingScale(3);
        item.setRoundingMode(RoundingMode.HALF_UP);
        item.setZeroPolicy(CustomerWeightZeroPolicy.ERROR);
        return item;
    }
}

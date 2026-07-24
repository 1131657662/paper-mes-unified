package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServicePricingFinalizationPolicyTest {

    @Test
    void serviceWithoutPrice_blocksSettlement() {
        ProcessStep step = serviceStep();
        step.setBillingMode(1);

        assertThrows(BusinessException.class,
                () -> ServicePricingFinalizationPolicy.requireFinalized(List.of(step)));
    }

    @Test
    void pricedFixedOrFreeService_allowsSettlement() {
        ProcessStep priced = serviceStep();
        priced.setBillingMode(1);
        priced.setBillingUnitPrice(new BigDecimal("50"));
        ProcessStep fixed = serviceStep();
        fixed.setBillingMode(3);
        fixed.setBillingAmount(new BigDecimal("200"));
        ProcessStep free = serviceStep();
        free.setBillingMode(4);

        assertDoesNotThrow(() -> ServicePricingFinalizationPolicy.requireFinalized(
                List.of(priced, fixed, free)));
    }

    private ProcessStep serviceStep() {
        ProcessStep step = new ProcessStep();
        step.setStepType(3);
        return step;
    }
}

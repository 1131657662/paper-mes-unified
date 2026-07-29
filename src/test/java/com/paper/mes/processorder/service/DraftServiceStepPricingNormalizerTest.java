package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessStepDTO;
import com.paper.mes.processorder.entity.ProcessStep;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DraftServiceStepPricingNormalizerTest {

    @Test
    void apply_freePricing_clearsQuantityFieldsAndStoresZeroAmount() {
        ProcessStep step = previouslyQuantityPricedStep();
        ProcessStepDTO request = request(ProcessStepPricingPolicy.FREE);

        DraftServiceStepPricingNormalizer.apply(step, request);

        assertThat(step.getBillingBasis()).isNull();
        assertThat(step.getUnitPrice()).isNull();
        assertThat(step.getBillingUnitPrice()).isNull();
        assertThat(step.getBillingQuantity()).isNull();
        assertThat(step.getBillingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void apply_fixedPricing_clearsQuantityFieldsAndKeepsFixedAmount() {
        ProcessStep step = previouslyQuantityPricedStep();
        ProcessStepDTO request = request(ProcessStepPricingPolicy.FIXED_AMOUNT);
        request.setBillingAmount(new BigDecimal("45.50"));

        DraftServiceStepPricingNormalizer.apply(step, request);

        assertThat(step.getBillingBasis()).isNull();
        assertThat(step.getUnitPrice()).isNull();
        assertThat(step.getBillingAmount()).isEqualByComparingTo("45.50");
    }

    private ProcessStep previouslyQuantityPricedStep() {
        ProcessStep step = new ProcessStep();
        step.setBillingBasis("PIECE");
        step.setUnitPrice(new BigDecimal("20"));
        step.setBillingUnitPrice(new BigDecimal("18"));
        step.setBillingQuantity(BigDecimal.ONE);
        step.setBillingAmount(new BigDecimal("20"));
        return step;
    }

    private ProcessStepDTO request(int mode) {
        ProcessStepDTO request = new ProcessStepDTO();
        request.setBillingMode(mode);
        request.setBillingBasis("PIECE");
        request.setUnitPrice(new BigDecimal("20"));
        return request;
    }
}

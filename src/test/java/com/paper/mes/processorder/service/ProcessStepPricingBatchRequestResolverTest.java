package com.paper.mes.processorder.service;

import com.paper.mes.processorder.dto.ProcessStepPricingBatchDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessStepPricingBatchRequestResolverTest {

    @Test
    void fixedTotal_isDistributedWithoutChangingTotal() {
        ProcessStepPricingBatchDTO.Group group = serviceGroup(3, ProcessStepPricingPolicy.FIXED_AMOUNT);
        group.setStepUuids(List.of("step-1", "step-2", "step-3"));
        group.setBillingAmount(new BigDecimal("100"));

        Map<String, ProcessStepPricingBatchRequestResolver.Change> changes =
                ProcessStepPricingBatchRequestResolver.resolve(request(group));

        assertThat(changes.values()).extracting(ProcessStepPricingBatchRequestResolver.Change::billingAmount)
                .containsExactly(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));
    }

    @Test
    void fixedTotal_withCents_isDistributedWithoutChangingTotal() {
        ProcessStepPricingBatchDTO.Group group = serviceGroup(3, ProcessStepPricingPolicy.FIXED_AMOUNT);
        group.setStepUuids(List.of("step-1", "step-2", "step-3"));
        group.setBillingAmount(new BigDecimal("100.01"));

        Map<String, ProcessStepPricingBatchRequestResolver.Change> changes =
                ProcessStepPricingBatchRequestResolver.resolve(request(group));

        assertThat(changes.values()).extracting(ProcessStepPricingBatchRequestResolver.Change::billingAmount)
                .containsExactly(new BigDecimal("33.34"), new BigDecimal("33.34"), new BigDecimal("33.33"));
    }

    @Test
    void serviceUnitPricing_preservesAutomaticQuantityBasis() {
        ProcessStepPricingBatchDTO.Group group = serviceGroup(4, ProcessStepPricingPolicy.STANDARD);
        group.setBillingBasis("TON");
        group.setBillingUnitPrice(new BigDecimal("85.5"));

        ProcessStepPricingBatchRequestResolver.Change change =
                ProcessStepPricingBatchRequestResolver.resolve(request(group)).get("step-1");

        assertThat(change.billingBasis()).isEqualTo("TON");
        assertThat(change.billingUnitPrice()).isEqualByComparingTo("85.5000");
    }

    private ProcessStepPricingBatchDTO.Group serviceGroup(int type, int mode) {
        ProcessStepPricingBatchDTO.Group group = new ProcessStepPricingBatchDTO.Group();
        group.setStepType(type);
        group.setStepUuids(List.of("step-1"));
        group.setRestoreStandard(false);
        group.setBillingMode(mode);
        return group;
    }

    private ProcessStepPricingBatchDTO request(ProcessStepPricingBatchDTO.Group group) {
        ProcessStepPricingBatchDTO dto = new ProcessStepPricingBatchDTO();
        dto.setGroups(List.of(group));
        return dto;
    }
}

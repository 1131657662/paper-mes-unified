package com.paper.mes.processorder.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SourceConsumptionRatioAllocatorTest {

    @Test
    void allocate_mixedExplicitAndLegacyRatios_consumesEachSourceAtMostOnce() {
        List<BigDecimal> ratios = SourceConsumptionRatioAllocator.allocate(List.of(
                new SourceConsumptionRatioAllocator.SourceRatio("source-a", new BigDecimal("50")),
                new SourceConsumptionRatioAllocator.SourceRatio("source-a", null),
                new SourceConsumptionRatioAllocator.SourceRatio("source-a", null)));

        assertThat(ratios).containsExactly(new BigDecimal("50"), new BigDecimal("50.00"), BigDecimal.ZERO);
    }

    @Test
    void allocate_explicitRatiosOverOneHundredPercent_rejectsInput() {
        assertThatThrownBy(() -> SourceConsumptionRatioAllocator.allocate(List.of(
                new SourceConsumptionRatioAllocator.SourceRatio("source-a", new BigDecimal("60")),
                new SourceConsumptionRatioAllocator.SourceRatio("source-a", new BigDecimal("41")))))
                .isInstanceOf(com.paper.mes.common.BusinessException.class)
                .hasMessageContaining("不能超过100");
    }
}

package com.paper.mes.settle.service;

import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettleCandidateAmountLoaderTest {

    @Test
    void load_withMultipleOrders_queriesStepsOnceAndGroupsAmounts() {
        ProcessStepMapper mapper = mock(ProcessStepMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                step("order-1", 1, "30"), step("order-1", 2, "20"), step("order-2", 1, "40")));
        SettleCandidateAmountLoader loader = new SettleCandidateAmountLoader(mapper);

        var result = loader.load(List.of(order("order-1", "50"), order("order-2", "40")));

        assertThat(result.get("order-1").saw()).isEqualByComparingTo("30.00");
        assertThat(result.get("order-1").rewind()).isEqualByComparingTo("20.00");
        assertThat(result.get("order-2").effectiveTotal()).isEqualByComparingTo("40.00");
        verify(mapper).selectList(any());
    }

    @Test
    void load_whenStepHasPricingOverride_exposesStandardAmountAndAdjustment() {
        ProcessStepMapper mapper = mock(ProcessStepMapper.class);
        ProcessStep step = step("order-1", 2, "100");
        step.setStandardStepAmount(new BigDecimal("370"));
        step.setPricingAdjustmentAmount(new BigDecimal("-270"));
        when(mapper.selectList(any())).thenReturn(List.of(step));

        var amount = new SettleCandidateAmountLoader(mapper)
                .load(List.of(order("order-1", "100"))).get("order-1");

        assertThat(amount.standardProcess()).isEqualByComparingTo("370.00");
        assertThat(amount.pricingAdjustment()).isEqualByComparingTo("-270.00");
        assertThat(amount.effectiveTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void load_withServiceSteps_includesServiceInCandidateTotal() {
        ProcessStepMapper mapper = mock(ProcessStepMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                step("order-1", 3, "60"), step("order-1", 4, "40")));

        var amount = new SettleCandidateAmountLoader(mapper)
                .load(List.of(order("order-1", "0"))).get("order-1");

        assertThat(amount.service()).isEqualByComparingTo("100.00");
        assertThat(amount.effectiveTotal()).isEqualByComparingTo("100.00");
    }

    private ProcessOrder order(String uuid, String total) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid(uuid);
        order.setTotalAmount(new BigDecimal(total));
        return order;
    }

    private ProcessStep step(String orderUuid, int type, String amount) {
        ProcessStep step = new ProcessStep();
        step.setOrderUuid(orderUuid);
        step.setStepType(type);
        step.setStepAmount(new BigDecimal(amount));
        return step;
    }
}

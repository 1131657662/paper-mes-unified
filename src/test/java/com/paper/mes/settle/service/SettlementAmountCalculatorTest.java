package com.paper.mes.settle.service;

import com.paper.mes.customer.entity.Customer;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessStep;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SettlementAmountCalculatorTest {

    @Test
    void calculate_whenInvoiceSelected_reusesUntaxedAmountAndAddsTax() {
        ProcessStepMapper mapper = mock(ProcessStepMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(step("order-1", 1, "40"), step("order-1", 2, "60")));
        SettlementAmountCalculator calculator = new SettlementAmountCalculator(mapper);
        ProcessOrder order = order("order-1", "100");
        order.setTaxRate(new BigDecimal("13"));

        SettlementAmountCalculator.Calculation result = calculator.calculate(List.of(order), 1, customer());

        assertThat(result.noTax()).isEqualByComparingTo("100.00");
        assertThat(result.tax()).isEqualByComparingTo("13.00");
        assertThat(result.total()).isEqualByComparingTo("113.00");
        assertThat(result.pendingPriceCount()).isZero();
    }

    @Test
    void calculate_whenOrderHasNoPrice_marksItPending() {
        ProcessStepMapper mapper = mock(ProcessStepMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        SettlementAmountCalculator calculator = new SettlementAmountCalculator(mapper);

        SettlementAmountCalculator.Calculation result = calculator.calculate(List.of(order("order-1", null)), 2, customer());

        assertThat(result.total()).isEqualByComparingTo("0.00");
        assertThat(result.pendingPriceCount()).isEqualTo(1);
    }

    @Test
    void calculate_whenRewindIsDiscounted_usesFinalAmountAndKeepsAdjustmentAudit() {
        ProcessStepMapper mapper = mock(ProcessStepMapper.class);
        ProcessStep step = step("order-1", 2, "100");
        step.setStandardQuantity(new BigDecimal("3.700"));
        step.setBillingQuantity(new BigDecimal("1.000"));
        step.setStandardStepAmount(new BigDecimal("370"));
        step.setPricingAdjustmentAmount(new BigDecimal("-270"));
        step.setPricingAdjustmentReason("客户仅加工20米");
        when(mapper.selectList(any())).thenReturn(List.of(step));

        SettlementAmountCalculator.Calculation result = new SettlementAmountCalculator(mapper)
                .calculate(List.of(order("order-1", null)), 2, customer());

        assertThat(result.rewind()).isEqualByComparingTo("100.00");
        assertThat(result.noTax()).isEqualByComparingTo("100.00");
        assertThat(result.details().getFirst().getStandardProcessAmount()).isEqualByComparingTo("370.00");
        assertThat(result.details().getFirst().getPricingAdjustmentAmount()).isEqualByComparingTo("-270.00");
        assertThat(result.details().getFirst().getPricingAdjustmentReason()).isEqualTo("客户仅加工20米");
    }

    private ProcessOrder order(String uuid, String noTaxAmount) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid(uuid);
        if (noTaxAmount != null) order.setTotalAmountNoTax(new BigDecimal(noTaxAmount));
        return order;
    }

    private ProcessStep step(String orderUuid, int type, String amount) {
        ProcessStep step = new ProcessStep();
        step.setOrderUuid(orderUuid);
        step.setStepType(type);
        step.setStepAmount(new BigDecimal(amount));
        return step;
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setTaxRate(new BigDecimal("13"));
        return customer;
    }
}

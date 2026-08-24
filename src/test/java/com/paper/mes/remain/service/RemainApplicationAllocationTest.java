package com.paper.mes.remain.service;

import com.paper.mes.remain.entity.RemainRegistrationLine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemainApplicationAllocationTest {

    @Test
    void allocate_partialTarget_keepsAmountAndWeightTotalsInSync() {
        RemainRegistrationLine first = line("100", "600");
        RemainRegistrationLine second = line("100", "400");

        RemainApplicationAllocation.AllocationResult result = RemainApplicationAllocation.allocate(
                List.of(first, second), bd("600"), bd("120"));

        assertEquals(0, bd("600").compareTo(result.amount()));
        assertEquals(0, bd("120").compareTo(result.weight()));
        assertEquals(0, bd("360").compareTo(first.getAppliedAmount()));
        assertEquals(0, bd("240").compareTo(second.getAppliedAmount()));
        assertEquals(0, bd("60").compareTo(first.getAppliedWeight()));
        assertEquals(0, bd("60").compareTo(second.getAppliedWeight()));
    }

    @Test
    void allocate_partialTarget_thenAdjustment_allocatesEveryRemainingAmountAndWeight() {
        RemainRegistrationLine first = line("40", "400");
        RemainRegistrationLine second = line("60", "600");

        RemainApplicationAllocation.AllocationResult current = RemainApplicationAllocation.allocate(
                List.of(first, second), bd("600"), bd("60"));
        RemainApplicationAllocation.AllocationResult adjustment = RemainApplicationAllocation.allocate(
                List.of(first, second), bd("400"), bd("40"));

        assertEquals(0, bd("600").compareTo(current.amount()));
        assertEquals(0, bd("60").compareTo(current.weight()));
        assertEquals(0, bd("400").compareTo(adjustment.amount()));
        assertEquals(0, bd("40").compareTo(adjustment.weight()));
        assertEquals(0, bd("1000").compareTo(first.getAppliedAmount().add(second.getAppliedAmount())));
        assertEquals(0, bd("100").compareTo(first.getAppliedWeight().add(second.getAppliedWeight())));
    }

    private static RemainRegistrationLine line(String weight, String amount) {
        RemainRegistrationLine line = new RemainRegistrationLine();
        line.setTransferredSystemWeight(bd(weight));
        line.setAmount(bd(amount));
        line.setAppliedAmount(BigDecimal.ZERO);
        line.setAppliedWeight(BigDecimal.ZERO);
        line.setRolledBackSystemWeight(BigDecimal.ZERO);
        line.setProcessedSystemWeight(BigDecimal.ZERO);
        return line;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}

package com.paper.mes.remain.service;

import com.paper.mes.remain.dto.RemainSaleLineDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RemainSaleAllocationTest {

    @Test
    void allocatesIntegerAmountByWeightAndPutsRemainderOnLastLine() {
        RemainSaleLineDTO first = line("lot-a", "40");
        RemainSaleLineDTO second = line("lot-b", "60");

        List<BigDecimal> amounts = RemainSaleAllocation.amounts(List.of(first, second),
                new BigDecimal("100"), new BigDecimal("1000"));

        assertThat(amounts).containsExactly(new BigDecimal("400"), new BigDecimal("600"));
    }

    @Test
    void allocatesRoundingRemainderWithoutChangingTotalAmount() {
        List<RemainSaleLineDTO> lines = List.of(line("lot-a", "1"), line("lot-b", "1"), line("lot-c", "1"));

        List<BigDecimal> amounts = RemainSaleAllocation.amounts(lines,
                new BigDecimal("3"), new BigDecimal("100"));

        assertThat(amounts).containsExactly(new BigDecimal("33"), new BigDecimal("33"), new BigDecimal("34"));
        assertThat(amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100");
    }

    private RemainSaleLineDTO line(String lotUuid, String weight) {
        RemainSaleLineDTO line = new RemainSaleLineDTO();
        line.setLotUuid(lotUuid);
        line.setSystemWeight(new BigDecimal(weight));
        return line;
    }
}

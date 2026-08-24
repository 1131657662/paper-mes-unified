package com.paper.mes.remain.service;

import com.paper.mes.remain.dto.RemainSaleLineDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

final class RemainSaleAllocation {
    private RemainSaleAllocation() {
    }

    static List<BigDecimal> amounts(List<RemainSaleLineDTO> lines, BigDecimal totalWeight,
                                    BigDecimal totalAmount) {
        List<BigDecimal> result = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < lines.size(); i++) {
            BigDecimal amount = i == lines.size() - 1
                    ? totalAmount.subtract(allocated)
                    : totalAmount.multiply(lines.get(i).getSystemWeight())
                    .divide(totalWeight, 0, RoundingMode.DOWN);
            result.add(amount.max(BigDecimal.ZERO));
            allocated = allocated.add(amount);
        }
        return result;
    }
}

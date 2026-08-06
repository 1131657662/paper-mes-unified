package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.OriginalRoll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

record DirectShipPiecePlan(List<BigDecimal> weights) {

    private static final int MAX_PIECES = 500;

    static DirectShipPiecePlan from(OriginalRoll source) {
        int count = source.getPieceNum() == null ? 1 : source.getPieceNum();
        if (count < 1 || count > MAX_PIECES) {
            throw new BusinessException("直发母卷件数必须在1到500之间");
        }
        BigDecimal total = source.getActualWeight();
        if (total == null) return new DirectShipPiecePlan(Collections.nCopies(count, null));
        BigDecimal unit = total.divide(BigDecimal.valueOf(count), 3, RoundingMode.HALF_UP);
        List<BigDecimal> weights = new ArrayList<>(Collections.nCopies(count, unit));
        weights.set(count - 1, remainder(total, unit, count));
        requirePositiveWeights(weights);
        return new DirectShipPiecePlan(weights);
    }

    int count() {
        return weights.size();
    }

    private static BigDecimal remainder(BigDecimal total, BigDecimal unit, int count) {
        return total.subtract(unit.multiply(BigDecimal.valueOf(count - 1)))
                .setScale(3, RoundingMode.HALF_UP);
    }

    private static void requirePositiveWeights(List<BigDecimal> weights) {
        if (weights.stream().anyMatch(weight -> weight.signum() <= 0)) {
            throw new BusinessException("直发复称总重量不足以按件分配，每件重量必须大于0");
        }
    }
}

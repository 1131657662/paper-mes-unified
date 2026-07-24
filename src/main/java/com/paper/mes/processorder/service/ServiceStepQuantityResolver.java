package com.paper.mes.processorder.service;

import com.paper.mes.processorder.calc.FeeCalculator;
import com.paper.mes.processorder.entity.OriginalRoll;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ServiceStepQuantityResolver {

    private ServiceStepQuantityResolver() {
    }

    public static BigDecimal resolve(String billingBasis, OriginalRoll roll) {
        if (roll == null || billingBasis == null) return null;
        if ("PIECE".equals(billingBasis)) {
            return BigDecimal.valueOf(roll.getPieceNum() == null ? 1 : roll.getPieceNum());
        }
        if (!"TON".equals(billingBasis)) return null;
        BigDecimal weight = sourceWeight(roll);
        return weight == null ? null : weight.divide(FeeCalculator.TON_DIVISOR, 3, RoundingMode.HALF_UP);
    }

    private static BigDecimal sourceWeight(OriginalRoll roll) {
        if (roll.getActualWeight() != null && roll.getActualWeight().signum() > 0) return roll.getActualWeight();
        if (roll.getTotalWeight() != null && roll.getTotalWeight().signum() > 0) return roll.getTotalWeight();
        if (roll.getRollWeight() == null) return null;
        int pieces = roll.getPieceNum() == null ? 1 : roll.getPieceNum();
        return roll.getRollWeight().multiply(BigDecimal.valueOf(pieces));
    }
}

package com.paper.mes.settle.service.impl;

import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.model.WeightStatus;

import java.math.BigDecimal;

/** Resolves the safest weight meaning to freeze into a settlement snapshot. */
final class SettleOriginalWeightStatus {

    private SettleOriginalWeightStatus() {
    }

    static String resolve(OriginalRoll roll) {
        String status = roll.getWeightStatus();
        if (WeightStatus.UNKNOWN.name().equalsIgnoreCase(status)) {
            return WeightStatus.UNKNOWN.name();
        }
        if (WeightStatus.ESTIMATED.name().equalsIgnoreCase(status)) {
            return WeightStatus.ESTIMATED.name();
        }
        if (positive(roll.getActualWeight())
                && (WeightStatus.MEASURED.name().equalsIgnoreCase(status)
                || status == null || status.isBlank())) {
            return WeightStatus.MEASURED.name();
        }
        return hasReferenceWeight(roll) ? WeightStatus.ESTIMATED.name() : WeightStatus.UNKNOWN.name();
    }

    private static boolean hasReferenceWeight(OriginalRoll roll) {
        return positive(roll.getTotalWeight()) || positive(roll.getRollWeight());
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}

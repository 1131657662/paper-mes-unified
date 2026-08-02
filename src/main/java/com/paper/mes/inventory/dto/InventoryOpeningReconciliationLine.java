package com.paper.mes.inventory.dto;

import lombok.Builder;

import java.math.BigDecimal;

/** Reconciliation contract for one switch-day opening line; no historical reconstruction is implied. */
@Builder
public record InventoryOpeningReconciliationLine(
        String finishRollUuid,
        BigDecimal projectedQuantity,
        BigDecimal openingQuantity,
        BigDecimal projectedWeight,
        BigDecimal openingWeight,
        BigDecimal quantityDifference,
        BigDecimal weightDifference) {

    public boolean matches() {
        return quantityDifference.signum() == 0 && weightDifference.signum() == 0;
    }
}

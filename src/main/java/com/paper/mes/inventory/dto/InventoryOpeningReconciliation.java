package com.paper.mes.inventory.dto;

import java.math.BigDecimal;
import java.util.List;

/** Result returned by a switch-day opening command caller after comparing the old projection. */
public record InventoryOpeningReconciliation(
        String switchUuid,
        List<InventoryOpeningReconciliationLine> lines,
        BigDecimal projectedQuantityTotal,
        BigDecimal openingQuantityTotal,
        BigDecimal projectedWeightTotal,
        BigDecimal openingWeightTotal,
        boolean matched) {

    public static InventoryOpeningReconciliation from(String switchUuid,
                                                       List<InventoryOpeningReconciliationLine> lines) {
        BigDecimal projectedQuantity = lines.stream()
                .map(InventoryOpeningReconciliationLine::projectedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal openingQuantity = lines.stream()
                .map(InventoryOpeningReconciliationLine::openingQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal projectedWeight = lines.stream()
                .map(InventoryOpeningReconciliationLine::projectedWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal openingWeight = lines.stream()
                .map(InventoryOpeningReconciliationLine::openingWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean matched = lines.stream().allMatch(InventoryOpeningReconciliationLine::matches);
        return new InventoryOpeningReconciliation(switchUuid, List.copyOf(lines), projectedQuantity,
                openingQuantity, projectedWeight, openingWeight, matched);
    }
}

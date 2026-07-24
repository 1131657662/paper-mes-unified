package com.paper.mes.processorder.dto;

import java.util.List;

/** Process capabilities exposed to order editing clients. */
public record ProcessCatalogVO(
        String uuid,
        int stepType,
        String code,
        String name,
        String category,
        String pricingStrategy,
        boolean producesInventoryOutput,
        boolean allowsLossRecording,
        boolean allowsMainProcess,
        List<ProcessCatalogUnitVO> units,
        List<Integer> billingModes
) {
    public boolean supportsUnit(String unitCode) {
        return units.stream().anyMatch(unit -> unit.code().equals(unitCode));
    }

    public boolean supportsBillingMode(int billingMode) {
        return billingModes.contains(billingMode);
    }
}

package com.paper.mes.inventory.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.inventory.dto.InventoryLedgerCommand;
import com.paper.mes.inventory.entity.InventoryLedgerEntry;

import java.math.BigDecimal;

record InventoryLedgerBalances(BigDecimal quantity, BigDecimal weight,
                               BigDecimal reservedQuantity, BigDecimal reservedWeight) {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    static InventoryLedgerBalances from(InventoryLedgerEntry previous) {
        if (previous == null) {
            return new InventoryLedgerBalances(ZERO, ZERO, ZERO, ZERO);
        }
        return new InventoryLedgerBalances(nz(previous.getQuantityAfter()), nz(previous.getWeightAfter()),
                nz(previous.getReservedQuantityAfter()), nz(previous.getReservedWeightAfter()));
    }

    InventoryLedgerBalances apply(InventoryLedgerCommand command) {
        InventoryLedgerBalances next = new InventoryLedgerBalances(
                quantity.add(command.getQuantityDelta()), weight.add(command.getWeightDelta()),
                reservedQuantity.add(command.getReservedQuantityDelta()),
                reservedWeight.add(command.getReservedWeightDelta()));
        if (next.quantity().signum() < 0 || next.weight().signum() < 0
                || next.reservedQuantity().signum() < 0 || next.reservedWeight().signum() < 0
                || next.availableQuantity().signum() < 0 || next.availableWeight().signum() < 0) {
            throw new BusinessException("inventory balance cannot be negative");
        }
        return next;
    }

    BigDecimal availableQuantity() {
        return quantity.subtract(reservedQuantity);
    }

    BigDecimal availableWeight() {
        return weight.subtract(reservedWeight);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}

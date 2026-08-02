package com.paper.mes.inventory.service;

import com.paper.mes.inventory.dto.InventoryLedgerCommand;
import com.paper.mes.inventory.entity.InventoryLedgerEntry;

import java.util.List;

public interface InventoryLedgerService {

    /** Append a non-opening event. OPENING_BALANCE must use openBalance explicitly. */
    InventoryLedgerEntry append(InventoryLedgerCommand command);

    /** Append the first event for a finish roll during the switch-day opening window. */
    InventoryLedgerEntry openBalance(InventoryLedgerCommand command);

    List<InventoryLedgerEntry> listByFinishUuid(String finishRollUuid);
}

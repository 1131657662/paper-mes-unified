package com.paper.mes.inventory.entity;

/** Events accepted by the append-only inventory ledger. */
public enum InventoryLedgerEventType {
    OPENING_BALANCE,
    RECEIPT,
    RESERVE,
    RELEASE,
    ISSUE,
    RETURN,
    SCRAP,
    ADJUSTMENT
}

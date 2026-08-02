package com.paper.mes.inventory.dto;

import com.paper.mes.inventory.entity.InventoryLedgerEventType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Write command for one inventory event. Values are validated again in the service. */
@Data
public class InventoryLedgerCommand {

    private String finishRollUuid;
    private InventoryLedgerEventType eventType;
    private String sourceBusinessType;
    private String sourceBusinessUuid;
    private BigDecimal quantityDelta;
    private BigDecimal weightDelta;
    private BigDecimal reservedQuantityDelta;
    private BigDecimal reservedWeightDelta;
    private String reason;
    private String operatorUuid;
    private String operatorName;
    private LocalDateTime occurredAt;
    private String idempotencyKey;
}

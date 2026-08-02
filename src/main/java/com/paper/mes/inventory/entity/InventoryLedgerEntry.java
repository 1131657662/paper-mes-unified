package com.paper.mes.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable finished-goods inventory event. There is intentionally no update/delete API. */
@Data
@TableName("biz_inventory_transaction")
public class InventoryLedgerEntry {

    @TableId
    private String uuid;
    private Long sequenceNo;
    private String finishRollUuid;
    private String eventType;
    private String sourceBusinessType;
    private String sourceBusinessUuid;
    private BigDecimal quantityDelta;
    private BigDecimal weightDelta;
    private BigDecimal reservedQuantityDelta;
    private BigDecimal reservedWeightDelta;
    private BigDecimal quantityBefore;
    private BigDecimal quantityAfter;
    private BigDecimal weightBefore;
    private BigDecimal weightAfter;
    private BigDecimal reservedQuantityBefore;
    private BigDecimal reservedQuantityAfter;
    private BigDecimal reservedWeightBefore;
    private BigDecimal reservedWeightAfter;
    private BigDecimal availableQuantityBefore;
    private BigDecimal availableQuantityAfter;
    private BigDecimal availableWeightBefore;
    private BigDecimal availableWeightAfter;
    private String reason;
    private String operatorUuid;
    private String operatorName;
    private LocalDateTime occurredAt;
    private String idempotencyKey;
    private String payloadHash;
    private LocalDateTime createdAt;
}

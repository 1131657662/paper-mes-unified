package com.paper.mes.inventory.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.inventory.dto.InventoryLedgerCommand;
import com.paper.mes.inventory.entity.InventoryLedgerEventType;
import com.paper.mes.inventory.entity.InventoryLedgerEntry;
import com.paper.mes.processorder.entity.FinishRoll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.util.StringUtils;

/** Builds the only business events allowed to enter the immutable inventory ledger. */
@Component
@RequiredArgsConstructor
public class InventoryLedgerBusinessRecorder {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private final InventoryLedgerService ledgerService;

    public InventoryLedgerEntry receipt(FinishRoll finish, String sourceUuid, String batchKey,
                                        LocalDateTime occurredAt) {
        String finishUuid = finishUuid(finish);
        if (!StringUtils.hasText(sourceUuid)) {
            throw new BusinessException("orderUuid is required for inventory receipt");
        }
        if (!StringUtils.hasText(batchKey)) {
            throw new BusinessException("batchKey is required for inventory receipt");
        }
        return append(finishUuid, InventoryLedgerEventType.RECEIPT,
                "PROCESS_ORDER_BACK_RECORD", sourceUuid.trim(), ONE,
                weight(finish.getActualWeight()), ZERO, ZERO, null,
                "RECEIPT:" + batchKey.trim() + ":" + finishUuid, occurredAt);
    }

    /**
     * Reverses the receipt created by a back-record batch that is being reopened.
     * The adjustment is append-only and keyed by the process-order version so a
     * later back-record of the same finish roll receives a different idempotency key.
     */
    public InventoryLedgerEntry reverseReceipt(FinishRoll finish, String orderUuid,
                                                String batchKey, LocalDateTime occurredAt) {
        String finishUuid = finishUuid(finish);
        if (!StringUtils.hasText(orderUuid)) {
            throw new BusinessException("orderUuid is required for inventory reversal");
        }
        if (!StringUtils.hasText(batchKey)) {
            throw new BusinessException("batchKey is required for inventory reversal");
        }
        BigDecimal receiptWeight = finish.getActualWeight();
        if (receiptWeight == null || receiptWeight.signum() <= 0) {
            throw new BusinessException("in-stock finish roll must have a positive actual weight");
        }
        String normalizedBatchKey = batchKey.trim();
        return append(finishUuid, InventoryLedgerEventType.ADJUSTMENT,
                "PROCESS_ORDER_BACK_RECORD_REOPEN", orderUuid.trim(), ONE.negate(),
                receiptWeight.negate(), ZERO, ZERO,
                "back-record reopen batch version " + normalizedBatchKey,
                "REVERSE_RECEIPT:" + normalizedBatchKey + ":" + finishUuid, occurredAt);
    }

    public InventoryLedgerEntry reserve(FinishRoll finish, String deliveryUuid,
                                        String detailUuid, BigDecimal weight, LocalDateTime occurredAt) {
        return append(finishUuid(finish), InventoryLedgerEventType.RESERVE,
                "DELIVERY_ORDER_RESERVE", deliveryUuid, ZERO, ZERO, ZERO,
                weight, null, "RESERVE:" + detailUuid, occurredAt);
    }

    public InventoryLedgerEntry release(FinishRoll finish, String deliveryUuid,
                                        String detailUuid, BigDecimal weight, LocalDateTime occurredAt) {
        return append(finishUuid(finish), InventoryLedgerEventType.RELEASE,
                "DELIVERY_ORDER_RELEASE", deliveryUuid, ZERO, ZERO, ZERO,
                weight.negate(), null, "RELEASE:" + detailUuid, occurredAt);
    }

    public InventoryLedgerEntry issue(FinishRoll finish, String deliveryUuid,
                                      String detailUuid, BigDecimal weight, boolean wholeRoll,
                                      Integer detailVersion, LocalDateTime occurredAt) {
        return append(finishUuid(finish), InventoryLedgerEventType.ISSUE,
                "DELIVERY_ORDER_ISSUE", deliveryUuid, wholeRoll ? BigDecimal.ONE.negate() : ZERO,
                weight.negate(), ZERO, weight.negate(), null,
                deliveryEventKey("ISSUE", detailUuid, detailVersion), occurredAt);
    }

    public InventoryLedgerEntry returned(FinishRoll finish, String deliveryUuid,
                                         String detailUuid, BigDecimal weight, boolean wholeRoll,
                                         Integer detailVersion, LocalDateTime occurredAt) {
        return append(finishUuid(finish), InventoryLedgerEventType.RETURN,
                "DELIVERY_ORDER_RETURN", deliveryUuid, wholeRoll ? BigDecimal.ONE : ZERO,
                weight, ZERO, weight, null,
                deliveryEventKey("RETURN", detailUuid, detailVersion), occurredAt);
    }

    public InventoryLedgerEntry scrap(FinishRoll finish, String requestUuid,
                                      String reason, BigDecimal weight, LocalDateTime occurredAt) {
        return append(finishUuid(finish), InventoryLedgerEventType.SCRAP,
                "FINISH_ROLL_SCRAP", requestUuid, BigDecimal.ONE.negate(), weight.negate(),
                ZERO, ZERO, reason, "SCRAP:" + requestUuid, occurredAt);
    }

    private InventoryLedgerEntry append(String finishUuid, InventoryLedgerEventType eventType,
                                        String sourceType, String sourceUuid, BigDecimal quantityDelta,
                                        BigDecimal weightDelta, BigDecimal reservedQuantityDelta,
                                        BigDecimal reservedWeightDelta, String reason,
                                        String idempotencyKey, LocalDateTime occurredAt) {
        InventoryLedgerCommand command = new InventoryLedgerCommand();
        command.setFinishRollUuid(finishUuid);
        command.setEventType(eventType);
        command.setSourceBusinessType(sourceType);
        command.setSourceBusinessUuid(sourceUuid);
        command.setQuantityDelta(quantityDelta);
        command.setWeightDelta(weightDelta);
        command.setReservedQuantityDelta(reservedQuantityDelta);
        command.setReservedWeightDelta(reservedWeightDelta);
        command.setReason(reason);
        command.setOccurredAt(occurredAt == null ? LocalDateTime.now() : occurredAt);
        command.setIdempotencyKey(idempotencyKey);
        return ledgerService.append(command);
    }

    private BigDecimal weight(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private String finishUuid(FinishRoll finish) {
        if (finish == null || !StringUtils.hasText(finish.getUuid())) {
            throw new BusinessException("finish roll is required for inventory event");
        }
        return finish.getUuid().trim();
    }

    private String deliveryEventKey(String eventType, String detailUuid, Integer detailVersion) {
        if (!StringUtils.hasText(detailUuid)) {
            throw new BusinessException("delivery detail is required for inventory event");
        }
        if (detailVersion == null || detailVersion < 1) {
            throw new BusinessException("delivery detail version is required for inventory event");
        }
        return eventType + ":" + detailUuid.trim() + ":" + detailVersion;
    }
}

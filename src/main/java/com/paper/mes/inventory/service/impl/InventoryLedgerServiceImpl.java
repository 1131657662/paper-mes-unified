package com.paper.mes.inventory.service.impl;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.inventory.dto.InventoryLedgerCommand;
import com.paper.mes.inventory.entity.InventoryLedgerEntry;
import com.paper.mes.inventory.entity.InventoryLedgerEventType;
import com.paper.mes.inventory.mapper.InventoryLedgerMapper;
import com.paper.mes.inventory.service.InventoryLedgerCommandValidator;
import com.paper.mes.inventory.service.InventoryLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryLedgerServiceImpl implements InventoryLedgerService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final InventoryLedgerMapper mapper;
    private final BusinessLockService businessLockService;

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public InventoryLedgerEntry append(InventoryLedgerCommand command) {
        return appendInternal(command, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public InventoryLedgerEntry openBalance(InventoryLedgerCommand command) {
        return appendInternal(command, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryLedgerEntry> listByFinishUuid(String finishRollUuid) {
        if (!StringUtils.hasText(finishRollUuid)) {
            throw new BusinessException("finishRollUuid is required");
        }
        return mapper.selectByFinishUuid(finishRollUuid.trim());
    }

    private InventoryLedgerEntry appendInternal(InventoryLedgerCommand raw, boolean openingCommand) {
        InventoryLedgerCommand command = normalize(raw);
        InventoryLedgerCommandValidator.validate(command, openingCommand);
        String payloadHash = InventoryLedgerPayloadHasher.hash(command);
        InventoryLedgerEntry existing = mapper.selectByIdempotencyKey(command.getIdempotencyKey());
        if (existing != null) {
            return requireSameCommand(existing, payloadHash);
        }

        businessLockService.lockFinishRolls(List.of(command.getFinishRollUuid()));
        existing = mapper.selectByIdempotencyKey(command.getIdempotencyKey());
        if (existing != null) {
            return requireSameCommand(existing, payloadHash);
        }
        InventoryLedgerEntry previous = mapper.selectLatestForUpdate(command.getFinishRollUuid());
        if (openingCommand && previous != null) {
            throw new BusinessException("opening balance already exists for finish roll");
        }
        if (!openingCommand && previous == null
                && command.getEventType() != InventoryLedgerEventType.RECEIPT) {
            throw new BusinessException("finish roll has no opening or receipt balance");
        }
        if (previous != null && previous.getOccurredAt() != null
                && command.getOccurredAt().isBefore(previous.getOccurredAt())) {
            throw new BusinessException("inventory event time cannot precede the latest ledger event");
        }
        InventoryLedgerEntry entry = buildEntry(command, previous, payloadHash);
        try {
            mapper.insert(entry);
            return entry;
        } catch (DuplicateKeyException duplicate) {
            InventoryLedgerEntry raced = mapper.selectByIdempotencyKey(command.getIdempotencyKey());
            if (raced != null) {
                return requireSameCommand(raced, payloadHash);
            }
            throw new BusinessException("inventory event conflicts with another command");
        }
    }

    private InventoryLedgerEntry buildEntry(InventoryLedgerCommand command,
                                             InventoryLedgerEntry previous,
                                             String payloadHash) {
        InventoryLedgerBalances before = InventoryLedgerBalances.from(previous);
        InventoryLedgerBalances after = before.apply(command);
        InventoryLedgerEntry entry = new InventoryLedgerEntry();
        entry.setUuid(UUID.randomUUID().toString());
        entry.setFinishRollUuid(command.getFinishRollUuid());
        entry.setEventType(command.getEventType().name());
        entry.setSourceBusinessType(command.getSourceBusinessType());
        entry.setSourceBusinessUuid(command.getSourceBusinessUuid());
        entry.setQuantityDelta(command.getQuantityDelta());
        entry.setWeightDelta(command.getWeightDelta());
        entry.setReservedQuantityDelta(command.getReservedQuantityDelta());
        entry.setReservedWeightDelta(command.getReservedWeightDelta());
        entry.setQuantityBefore(before.quantity());
        entry.setQuantityAfter(after.quantity());
        entry.setWeightBefore(before.weight());
        entry.setWeightAfter(after.weight());
        entry.setReservedQuantityBefore(before.reservedQuantity());
        entry.setReservedQuantityAfter(after.reservedQuantity());
        entry.setReservedWeightBefore(before.reservedWeight());
        entry.setReservedWeightAfter(after.reservedWeight());
        entry.setAvailableQuantityBefore(before.availableQuantity());
        entry.setAvailableQuantityAfter(after.availableQuantity());
        entry.setAvailableWeightBefore(before.availableWeight());
        entry.setAvailableWeightAfter(after.availableWeight());
        entry.setReason(normalizeReason(command.getReason()));
        entry.setOperatorUuid(command.getOperatorUuid());
        entry.setOperatorName(command.getOperatorName());
        entry.setOccurredAt(command.getOccurredAt());
        entry.setIdempotencyKey(command.getIdempotencyKey());
        entry.setPayloadHash(payloadHash);
        entry.setCreatedAt(LocalDateTime.now());
        return entry;
    }

    private InventoryLedgerCommand normalize(InventoryLedgerCommand raw) {
        if (raw == null) {
            return null;
        }
        InventoryLedgerCommand command = new InventoryLedgerCommand();
        command.setFinishRollUuid(trim(raw.getFinishRollUuid()));
        command.setEventType(raw.getEventType());
        command.setSourceBusinessType(trim(raw.getSourceBusinessType()));
        command.setSourceBusinessUuid(trim(raw.getSourceBusinessUuid()));
        command.setQuantityDelta(raw.getQuantityDelta());
        command.setWeightDelta(raw.getWeightDelta());
        command.setReservedQuantityDelta(raw.getReservedQuantityDelta() == null ? ZERO : raw.getReservedQuantityDelta());
        command.setReservedWeightDelta(raw.getReservedWeightDelta() == null ? ZERO : raw.getReservedWeightDelta());
        command.setReason(trim(raw.getReason()));
        command.setOperatorUuid(resolveOperatorUuid(raw.getOperatorUuid()));
        command.setOperatorName(resolveOperatorName(raw.getOperatorName()));
        command.setOccurredAt(raw.getOccurredAt() == null ? LocalDateTime.now() : raw.getOccurredAt());
        command.setIdempotencyKey(trim(raw.getIdempotencyKey()));
        return command;
    }

    private String resolveOperatorName(String requested) {
        if (StringUtils.hasText(requested)) {
            return requested.trim();
        }
        return AuthContextHolder.currentDisplayName();
    }

    private String resolveOperatorUuid(String requested) {
        if (StringUtils.hasText(requested)) {
            return requested.trim();
        }
        if (AuthContextHolder.getCurrentUser() == null) {
            return null;
        }
        return trim(AuthContextHolder.getCurrentUser().getUuid());
    }

    private InventoryLedgerEntry requireSameCommand(InventoryLedgerEntry existing, String payloadHash) {
        if (!Objects.equals(existing.getPayloadHash(), payloadHash)) {
            throw new BusinessException("idempotency key was already used by another inventory command");
        }
        return existing;
    }

    private String normalizeReason(String reason) {
        return StringUtils.hasText(reason) ? reason.trim() : null;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }
}

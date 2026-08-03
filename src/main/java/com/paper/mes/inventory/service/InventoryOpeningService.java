package com.paper.mes.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.ResultCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.inventory.dto.InventoryOpeningReconciliation;
import com.paper.mes.inventory.dto.InventoryOpeningReconciliationLine;
import com.paper.mes.inventory.dto.InventoryOpeningRequest;
import com.paper.mes.inventory.dto.InventoryLedgerCommand;
import com.paper.mes.inventory.entity.InventoryLedgerEntry;
import com.paper.mes.inventory.entity.InventoryLedgerEventType;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryOpeningService {

    private static final int IN_STOCK = 2;
    private static final int STOCK_LOCK_ACTIVE = 1;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final FinishRollMapper finishRollMapper;
    private final InventoryOpeningReservationReader reservationReader;
    private final BusinessLockService businessLockService;
    private final InventoryLedgerService ledgerService;
    private final OperationLogService operationLogService;

    // The preview is write-free at the business level, but must hold the same
    // row and switch locks as the opening command while the projection is read.
    @Transactional(rollbackFor = Exception.class)
    public InventoryOpeningReconciliation previewCurrentProjection(InventoryOpeningRequest request) {
        validateRequest(request);
        List<FinishRoll> finishes = lockAndReloadFinishes();
        Map<String, BigDecimal> reservedByFinish = reservationReader.read(finishes);
        List<InventoryOpeningReconciliationLine> lines = finishes.stream()
                .map(finish -> previewOne(finish, reservedByFinish.getOrDefault(finish.getUuid(), ZERO)))
                .toList();
        return InventoryOpeningReconciliation.preview(request.getSwitchUuid().trim(), lines);
    }

    @Transactional(rollbackFor = Exception.class)
    public InventoryOpeningReconciliation openCurrentProjection(InventoryOpeningRequest request) {
        validateRequest(request);
        List<FinishRoll> finishes = lockAndReloadFinishes();
        Map<String, BigDecimal> reservedByFinish = reservationReader.read(finishes);
        List<InventoryOpeningReconciliationLine> lines = new ArrayList<>(finishes.size());
        for (FinishRoll finish : finishes) {
            lines.add(openOne(finish, reservedByFinish.getOrDefault(finish.getUuid(), ZERO), request));
        }
        InventoryOpeningReconciliation reconciliation =
                InventoryOpeningReconciliation.from(request.getSwitchUuid().trim(), lines);
        requireMatched(reconciliation);
        operationLogService.record(OperationLogService.BIZ_TYPE_INVENTORY, request.getSwitchUuid().trim(),
                request.getSwitchUuid().trim(), OperationLogService.ACTION_INVENTORY_OPENING, null,
                reconciliation.auditSummary());
        return reconciliation;
    }

    private List<FinishRoll> lockAndReloadFinishes() {
        List<FinishRoll> finishes = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .orderByAsc(FinishRoll::getUuid));
        businessLockService.lockFinishRolls(finishes.stream().map(FinishRoll::getUuid).toList());
        // Keep the lock order identical to normal ledger writes: roll rows first, switch gate second.
        businessLockService.lockInventorySwitch();
        return reloadFinishes();
    }

    private InventoryOpeningReconciliationLine previewOne(FinishRoll finish, BigDecimal reserved) {
        BigDecimal available = availableProjection(finish);
        BigDecimal physical = available.add(reserved);
        BigDecimal quantity = physical.signum() > 0 ? BigDecimal.ONE : ZERO;
        return InventoryOpeningReconciliationLine.builder()
                .finishRollUuid(finish.getUuid())
                .projectedQuantity(quantity)
                .openingQuantity(quantity)
                .projectedWeight(available)
                .openingWeight(available)
                .quantityDifference(ZERO)
                .weightDifference(ZERO)
                .build();
    }

    private void validateRequest(InventoryOpeningRequest request) {
        if (request == null) {
            throw new BusinessException("inventory opening command is required");
        }
        requireText(request.getSwitchUuid(), "switchUuid");
    }

    private InventoryOpeningReconciliationLine openOne(FinishRoll finish, BigDecimal reserved,
                                                       InventoryOpeningRequest request) {
        BigDecimal available = availableProjection(finish);
        BigDecimal physical = available.add(reserved);
        BigDecimal quantity = physical.signum() > 0 ? BigDecimal.ONE : ZERO;
        InventoryLedgerCommand command = new InventoryLedgerCommand();
        command.setFinishRollUuid(finish.getUuid());
        command.setEventType(InventoryLedgerEventType.OPENING_BALANCE);
        command.setSourceBusinessType("INVENTORY_SWITCH");
        command.setSourceBusinessUuid(request.getSwitchUuid().trim());
        command.setQuantityDelta(quantity);
        command.setWeightDelta(physical);
        command.setReservedQuantityDelta(ZERO);
        command.setReservedWeightDelta(reserved);
        command.setReason("切换日开账");
        command.setOccurredAt(request.getOccurredAt());
        command.setIdempotencyKey("OPENING:" + request.getSwitchUuid().trim() + ":" + finish.getUuid());
        InventoryLedgerEntry entry = ledgerService.openBalance(command);
        BigDecimal quantityDiff = entry.getAvailableQuantityAfter().subtract(quantity);
        BigDecimal weightDiff = entry.getAvailableWeightAfter().subtract(available);
        return InventoryOpeningReconciliationLine.builder()
                .finishRollUuid(finish.getUuid())
                .projectedQuantity(quantity)
                .openingQuantity(entry.getAvailableQuantityAfter())
                .projectedWeight(available)
                .openingWeight(entry.getAvailableWeightAfter())
                .quantityDifference(quantityDiff)
                .weightDifference(weightDiff)
                .build();
    }

    private List<FinishRoll> reloadFinishes() {
        return finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .orderByAsc(FinishRoll::getUuid));
    }

    private BigDecimal availableProjection(FinishRoll finish) {
        if (!Integer.valueOf(IN_STOCK).equals(finish.getFinishStatus())) {
            return ZERO;
        }
        BigDecimal remaining = finish.getRemainingWeight();
        if (remaining == null) {
            remaining = finish.getActualWeight();
        }
        if (remaining == null || remaining.signum() < 0) {
            throw new BusinessException("成品卷库存投影无效：" + finish.getFinishRollNo());
        }
        return remaining;
    }

    private void requireMatched(InventoryOpeningReconciliation reconciliation) {
        if (reconciliation.matched()) {
            return;
        }
        String differences = reconciliation.lines().stream()
                .filter(line -> !line.matches())
                .limit(20)
                .map(line -> line.finishRollUuid() + "(quantity=" + line.quantityDifference()
                        + ",weight=" + line.weightDifference() + ")")
                .collect(Collectors.joining(", "));
        throw new BusinessException(ResultCode.CONFLICT, ErrorCode.E004.getCode(),
                "inventory opening reconciliation mismatch: " + differences);
    }

    private void requireText(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(name + "不能为空");
        }
    }
}

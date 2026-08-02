package com.paper.mes.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.inventory.dto.InventoryOpeningReconciliation;
import com.paper.mes.inventory.dto.InventoryOpeningReconciliationLine;
import com.paper.mes.inventory.dto.InventoryOpeningRequest;
import com.paper.mes.inventory.dto.InventoryLedgerCommand;
import com.paper.mes.inventory.entity.InventoryLedgerEntry;
import com.paper.mes.inventory.entity.InventoryLedgerEventType;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryOpeningService {

    private static final int IN_STOCK = 2;
    private static final int STOCK_LOCK_ACTIVE = 1;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final FinishRollMapper finishRollMapper;
    private final DeliveryDetailMapper deliveryDetailMapper;
    private final BusinessLockService businessLockService;
    private final InventoryLedgerService ledgerService;

    @Transactional(rollbackFor = Exception.class)
    public InventoryOpeningReconciliation openCurrentProjection(InventoryOpeningRequest request) {
        if (request == null) {
            throw new BusinessException("inventory opening command is required");
        }
        requireText(request.getSwitchUuid(), "switchUuid");
        businessLockService.lockInventorySwitch();
        List<FinishRoll> finishes = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .orderByAsc(FinishRoll::getUuid));
        businessLockService.lockFinishRolls(finishes.stream().map(FinishRoll::getUuid).toList());
        finishes = reloadFinishes();
        Map<String, BigDecimal> reservedByFinish = activeReservations(finishes);
        List<InventoryOpeningReconciliationLine> lines = new ArrayList<>(finishes.size());
        for (FinishRoll finish : finishes) {
            lines.add(openOne(finish, reservedByFinish.getOrDefault(finish.getUuid(), ZERO), request));
        }
        return InventoryOpeningReconciliation.from(request.getSwitchUuid(), lines);
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

    private Map<String, BigDecimal> activeReservations(List<FinishRoll> finishes) {
        if (finishes.isEmpty()) {
            return Map.of();
        }
        List<String> finishUuids = finishes.stream().map(FinishRoll::getUuid).toList();
        Map<String, BigDecimal> result = new HashMap<>();
        List<DeliveryDetail> details = deliveryDetailMapper.selectList(new LambdaQueryWrapper<DeliveryDetail>()
                .in(DeliveryDetail::getFinishUuid, finishUuids)
                .eq(DeliveryDetail::getStockLockStatus, STOCK_LOCK_ACTIVE));
        for (DeliveryDetail detail : details) {
            result.merge(detail.getFinishUuid(), nz(detail.getOutWeight()), BigDecimal::add);
        }
        return result;
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

    private BigDecimal nz(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private void requireText(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(name + "不能为空");
        }
    }
}

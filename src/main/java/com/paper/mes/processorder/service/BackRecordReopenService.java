package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.inventory.service.InventoryLedgerBusinessRecorder;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BackRecordReopenService {

    private static final int CHECKED = 1;
    private static final int ROLL_PROCESSING = 2;
    private static final int FINISH_PENDING = 1;
    private static final int FINISH_IN_STOCK = 2;
    private static final int FINISH_OUT = 3;
    private static final int ROLL_NO_PRE = 1;
    private static final int ROLL_NO_VOID = 3;
    private static final int RESULT_PLANNED = 1;
    private static final int RESULT_NOT_PRODUCED = 3;
    private static final int RESULT_ADDED = 4;
    private static final int SOURCE_DIRECT_SHIP = 2;

    private final OriginalRollMapper rollMapper;
    private final FinishRollMapper finishRollMapper;
    private final FinishOriginalRelMapper relationMapper;
    private final DeliveryDetailMapper deliveryDetailMapper;
    private final BusinessLockService businessLockService;
    private final InventoryLedgerBusinessRecorder inventoryLedgerRecorder;

    @Transactional(rollbackFor = Exception.class)
    public int reopen(String orderUuid, Collection<String> requestedRollUuids,
                      String operator, Integer orderVersion) {
        if (orderVersion == null) {
            throw new BusinessException(ErrorCode.E006, "加工单版本缺失，无法安全撤回库存流水");
        }
        return reopen(orderUuid, requestedRollUuids, operator, String.valueOf(orderVersion));
    }

    private int reopen(String orderUuid, Collection<String> requestedRollUuids,
                       String operator, String batchKey) {
        List<OriginalRoll> allRolls = rolls(orderUuid);
        List<OriginalRoll> checkedRolls = allRolls.stream()
                .filter(roll -> Integer.valueOf(CHECKED).equals(roll.getIsChecked()))
                .filter(this::isActiveRoll)
                .toList();
        Set<String> selected = requireSelectedRolls(checkedRolls, requestedRollUuids);
        List<FinishOriginalRel> relations = relations(orderUuid);
        Set<String> finishUuids = relatedFinishUuids(selected, relations);
        long activeRollCount = allRolls.stream().filter(this::isActiveRoll).count();
        if (selected.size() == activeRollCount) {
            includeUnlinkedFinishes(orderUuid, relations, finishUuids);
        }
        businessLockService.lockFinishRolls(finishUuids);
        List<FinishRoll> finishes = lockedFinishes(orderUuid, finishUuids);
        requireNoDeliveryActivity(finishes);
        reopenFinishes(finishes, orderUuid, batchKey, operator);
        reopenRolls(selected, operator);
        return selected.size();
    }

    public int reopen(String orderUuid, String operator, Integer orderVersion) {
        return reopen(orderUuid, null, operator, orderVersion);
    }

    /**
     * Reverses every receipt still represented as in-stock for an order without
     * changing production status. Rollback cleanup calls this before clearing
     * actual weights, so the ledger remains the source of inventory truth.
     */
    @Transactional(rollbackFor = Exception.class)
    public int reverseStockInReceipts(String orderUuid, Integer orderVersion) {
        if (orderVersion == null) {
            throw new BusinessException(ErrorCode.E006, "加工单版本缺失，无法安全撤回库存流水");
        }
        List<FinishRoll> allFinishes = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, orderUuid));
        Set<String> finishUuids = allFinishes.stream()
                .map(FinishRoll::getUuid)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        businessLockService.lockFinishRolls(finishUuids.stream().toList());
        List<FinishRoll> finishes = lockedFinishes(orderUuid, finishUuids);
        requireNoDeliveryActivity(finishes);
        int reversed = 0;
        for (FinishRoll finish : finishes) {
            if (!Integer.valueOf(FINISH_IN_STOCK).equals(finish.getFinishStatus())) {
                continue;
            }
            inventoryLedgerRecorder.reverseReceipt(finish, orderUuid,
                    String.valueOf(orderVersion), LocalDateTime.now());
            reversed++;
        }
        return reversed;
    }

    private List<OriginalRoll> rolls(String orderUuid) {
        return rollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                .eq(OriginalRoll::getOrderUuid, orderUuid)
                .orderByAsc(OriginalRoll::getRowSort));
    }

    private Set<String> requireSelectedRolls(List<OriginalRoll> checkedRolls,
                                             Collection<String> requestedRollUuids) {
        Map<String, OriginalRoll> checkedByUuid = new LinkedHashMap<>();
        checkedRolls.forEach(roll -> checkedByUuid.put(roll.getUuid(), roll));
        Set<String> selected = requestedRollUuids == null
                ? new LinkedHashSet<>(checkedByUuid.keySet())
                : new LinkedHashSet<>(requestedRollUuids);
        if (selected.isEmpty()) {
            throw new BusinessException("没有可撤回的已回录母卷");
        }
        if (requestedRollUuids != null && selected.size() != requestedRollUuids.size()) {
            throw new BusinessException("撤回母卷不能重复");
        }
        for (String uuid : selected) {
            if (!checkedByUuid.containsKey(uuid)) {
                throw new BusinessException(ErrorCode.E003, "母卷不属于当前加工单或尚未回录");
            }
        }
        return selected;
    }

    private List<FinishOriginalRel> relations(String orderUuid) {
        return relationMapper.selectList(new LambdaQueryWrapper<FinishOriginalRel>()
                .eq(FinishOriginalRel::getOrderUuid, orderUuid));
    }

    private Set<String> relatedFinishUuids(Set<String> selected,
                                           List<FinishOriginalRel> relations) {
        Map<String, Set<String>> sourcesByFinish = new LinkedHashMap<>();
        for (FinishOriginalRel relation : relations) {
            sourcesByFinish.computeIfAbsent(relation.getFinishUuid(), ignored -> new LinkedHashSet<>())
                    .add(relation.getOriginalUuid());
        }
        Set<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> entry : sourcesByFinish.entrySet()) {
            if (entry.getValue().stream().noneMatch(selected::contains)) continue;
            if (!selected.containsAll(entry.getValue())) {
                throw new BusinessException(ErrorCode.E003, "合并复卷的全部来源母卷必须一起撤回");
            }
            result.add(entry.getKey());
        }
        return result;
    }

    private void includeUnlinkedFinishes(String orderUuid, List<FinishOriginalRel> relations,
                                         Set<String> finishUuids) {
        Set<String> linked = relations.stream().map(FinishOriginalRel::getFinishUuid)
                .collect(java.util.stream.Collectors.toSet());
        finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                        .eq(FinishRoll::getOrderUuid, orderUuid)
                        .and(wrapper -> wrapper.isNull(FinishRoll::getSourceType)
                                .or().ne(FinishRoll::getSourceType, SOURCE_DIRECT_SHIP)))
                .stream()
                .map(FinishRoll::getUuid).filter(uuid -> !linked.contains(uuid))
                .forEach(finishUuids::add);
    }

    private boolean isActiveRoll(OriginalRoll roll) {
        Integer status = roll.getRollStatus();
        return roll.getDispositionAction() == null
                && !Integer.valueOf(4).equals(status)
                && !Integer.valueOf(5).equals(status);
    }

    private List<FinishRoll> lockedFinishes(String orderUuid, Set<String> finishUuids) {
        if (finishUuids.isEmpty()) return List.of();
        return finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, orderUuid)
                .in(FinishRoll::getUuid, finishUuids)
                .orderByAsc(FinishRoll::getRowSort));
    }

    private void requireNoDeliveryActivity(List<FinishRoll> finishes) {
        if (finishes.stream().anyMatch(finish -> Integer.valueOf(FINISH_OUT).equals(finish.getFinishStatus()))) {
            throw new BusinessException(ErrorCode.E003, "本批已有成品出库，不能撤回回录");
        }
        List<String> finishUuids = finishes.stream().map(FinishRoll::getUuid).toList();
        if (finishUuids.isEmpty()) return;
        if (deliveryDetailMapper.countBlockingDeliveryActivity(finishUuids) > 0) {
            throw new BusinessException(ErrorCode.E003, "本批成品已进入出库流程，请先处理对应出库单");
        }
    }

    private void reopenFinishes(List<FinishRoll> finishes, String orderUuid,
                                String batchKey, String operator) {
        for (FinishRoll finish : finishes) {
            if (isVoidedActualAddition(finish)) continue;
            if (Integer.valueOf(FINISH_IN_STOCK).equals(finish.getFinishStatus())) {
                inventoryLedgerRecorder.reverseReceipt(finish, orderUuid, batchKey, LocalDateTime.now());
            }
            boolean restorePlanned = Integer.valueOf(RESULT_NOT_PRODUCED).equals(finish.getProductionResult())
                    || (Integer.valueOf(1).equals(finish.getIsSpare())
                    && Integer.valueOf(ROLL_NO_VOID).equals(finish.getRollNoStatus()));
            LambdaUpdateWrapper<FinishRoll> update = new LambdaUpdateWrapper<FinishRoll>()
                    .eq(FinishRoll::getUuid, finish.getUuid())
                    .eq(finish.getVersion() != null, FinishRoll::getVersion, finish.getVersion())
                    .set(FinishRoll::getFinishStatus, FINISH_PENDING)
                    .set(FinishRoll::getStockInTime, null)
                    .set(FinishRoll::getUpdateBy, operator)
                    .set(FinishRoll::getUpdateTime, LocalDateTime.now())
                    .setSql("version = COALESCE(version, 0) + 1");
            if (restorePlanned) restorePlannedFinish(update);
            ConcurrencyGuard.requireRowUpdated(finishRollMapper.update(null, update));
        }
    }

    private boolean isVoidedActualAddition(FinishRoll finish) {
        return Integer.valueOf(RESULT_ADDED).equals(finish.getProductionResult())
                && Integer.valueOf(ROLL_NO_VOID).equals(finish.getRollNoStatus());
    }

    private void restorePlannedFinish(LambdaUpdateWrapper<FinishRoll> update) {
        update.set(FinishRoll::getRollNoStatus, ROLL_NO_PRE)
                .set(FinishRoll::getProductionResult, RESULT_PLANNED)
                .set(FinishRoll::getProductionAdjustmentReason, null)
                .set(FinishRoll::getActualWeight, null)
                .set(FinishRoll::getRemainingWeight, null)
                .set(FinishRoll::getScrapWeight, null)
                .set(FinishRoll::getIsAbnormal, 0)
                .set(FinishRoll::getAbnormalType, null)
                .set(FinishRoll::getActualRemark, null);
    }

    private void reopenRolls(Set<String> selected, String operator) {
        int updated = rollMapper.update(null, new LambdaUpdateWrapper<OriginalRoll>()
                .in(OriginalRoll::getUuid, selected)
                .eq(OriginalRoll::getIsChecked, CHECKED)
                .set(OriginalRoll::getIsChecked, 0)
                .set(OriginalRoll::getRollStatus, ROLL_PROCESSING)
                .set(OriginalRoll::getCheckUser, null)
                .set(OriginalRoll::getCheckTime, null)
                .set(OriginalRoll::getUpdateBy, operator)
                .set(OriginalRoll::getUpdateTime, LocalDateTime.now())
                .setSql("version = COALESCE(version, 0) + 1"));
        if (updated != selected.size()) {
            throw new BusinessException(ErrorCode.E006, "回录状态已变化，请刷新后重试");
        }
    }
}

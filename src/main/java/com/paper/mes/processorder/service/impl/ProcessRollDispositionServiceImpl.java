package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.inventory.service.InventoryLedgerBusinessRecorder;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.dto.ProcessOrderCreateDTO;
import com.paper.mes.processorder.dto.ProcessRollDispositionDTO;
import com.paper.mes.processorder.dto.ProcessRollDispositionVO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessRollDisposition;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessRollDispositionMapper;
import com.paper.mes.processorder.model.ProcessRollDispositionAction;
import com.paper.mes.processorder.model.ProcessRollDispositionStatus;
import com.paper.mes.processorder.model.WeightStatus;
import com.paper.mes.processorder.service.BackRecordDirectShipRecorder;
import com.paper.mes.processorder.service.BackRecordWarehousePolicy;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.processorder.service.ProcessRollDispositionPolicy;
import com.paper.mes.processorder.service.ProcessRollDispositionService;
import com.paper.mes.processorder.service.ProcessRollSplitFactory;
import com.paper.mes.processorder.service.ProcessRouteCleanupService;
import com.paper.mes.processorder.service.ProcessRouteContext;
import com.paper.mes.processorder.service.RollNoSequenceService;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/** Transactional command handler for post-issue source-roll disposition. */
@Service
@RequiredArgsConstructor
public class ProcessRollDispositionServiceImpl implements ProcessRollDispositionService {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int ROLL_DIRECT = 4;
    private static final int CHECKED = 1;
    private static final int PROCESS_MODE_DIRECT_SHIP = 3;

    private final OriginalRollMapper rollMapper;
    private final FinishRollMapper finishMapper;
    private final FinishOriginalRelMapper relationMapper;
    private final ProcessRollDispositionMapper dispositionMapper;
    private final DeliveryDetailMapper deliveryDetailMapper;
    private final SettleDetailMapper settleDetailMapper;
    private final ProcessOrderService orderService;
    private final ProcessRouteCleanupService routeCleanupService;
    private final BackRecordDirectShipRecorder directShipRecorder;
    private final BackRecordWarehousePolicy backRecordWarehousePolicy;
    private final InventoryLedgerBusinessRecorder inventoryRecorder;
    private final OperationLogService operationLogService;
    private final BusinessLockService businessLockService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProcessRollDispositionVO dispose(String rollUuid, ProcessRollDispositionDTO dto) {
        ProcessRollDispositionPolicy.requireCommand(dto);
        OriginalRoll roll = requireRoll(rollUuid);
        businessLockService.lockProcessOrders(List.of(roll.getOrderUuid()));
        // The initial read only identifies the order to lock. Re-read the roll after
        // the lock so a concurrent back-record cannot be mistaken for an editable roll.
        roll = requireRoll(rollUuid);
        ProcessRollDisposition existing = findByRoll(rollUuid);
        if (existing != null) {
            if (dto.getRequestId().trim().equals(existing.getRequestId())) return result(existing);
            throw new BusinessException("母卷已经处置，不能使用新的请求号重复操作");
        }
        ProcessOrder order = requireOrder(roll.getOrderUuid());
        ProcessRollDispositionPolicy.requireOrderEditable(order);
        requireVersion(order, dto.getExpectedOrderVersion());
        ProcessRollDispositionPolicy.requireRollEditable(roll);
        requireNoDownstreamReference(order, roll);
        ensureRequestFree(dto.getRequestId(), rollUuid);

        String warehouse = resolveWarehouse(order, dto);
        LocalDateTime now = LocalDateTime.now();
        ProcessRollDisposition applied = switch (dto.getAction()) {
            case CANCEL -> applyCancel(order, roll, dto, now);
            case DIRECT_SHIP -> applyDirectShip(order, roll, dto, warehouse, now);
            case SPLIT_TO_ORDER -> applySplit(order, roll, dto, now);
        };
        ProcessOrder currentOrder = touchOrder(order.getUuid());
        applied.setRequestId(dto.getRequestId().trim());
        applied.setReason(dto.getReason().trim());
        applied.setOperator(currentOperator());
        applied.setOperateTime(now);
        applied.setSourceOrderVersion(currentOrder.getVersion());
        applied.setSourceRollVersion(roll.getVersion());
        dispositionMapper.insert(applied);
        operationLogService.record(OperationLogService.BIZ_TYPE_ORDER, order.getUuid(), order.getOrderNo(),
                OperationLogService.ACTION_ROLL_DISPOSITION, currentOperator(),
                dto.getAction().name() + ":" + rollLabel(roll) + ":" + dto.getReason().trim());
        return result(applied);
    }

    private ProcessRollDisposition applyCancel(ProcessOrder order, OriginalRoll roll,
                                                ProcessRollDispositionDTO dto, LocalDateTime now) {
        routeCleanupService.clearExistingRoute(new ProcessRouteContext(order, roll));
        markDisposed(roll, ProcessRollDispositionAction.CANCEL);
        orderService.calcFee(order.getUuid());
        return base(order, roll, dto.getAction());
    }

    private ProcessRollDisposition applyDirectShip(ProcessOrder order, OriginalRoll roll,
                                                   ProcessRollDispositionDTO dto, String warehouse,
                                                   LocalDateTime now) {
        routeCleanupService.clearExistingRoute(new ProcessRouteContext(order, roll));
        // Persist the first selected warehouse on the order before creating the
        // direct-ship product so later rolls cannot be sent to a different one.
        if (!warehouse.equals(order.getWarehouseUuid())) {
            order.setWarehouseUuid(warehouse);
            ConcurrencyGuard.requireUpdated(orderService.updateById(order));
        }
        OriginalRoll source = copyForDirectShip(roll, dto.getActualWeight());
        BackRecordDirectShipRecorder.Result direct = directShipRecorder.record(
                order, List.of(source), List.of(source));
        if (direct.finishes().isEmpty()) throw new BusinessException("直发未生成可入库成品");
        for (FinishRoll finish : direct.finishes()) {
            finish.setWarehouseUuid(warehouse);
            finish.setStockInTime(now);
            ConcurrencyGuard.requireRowUpdated(finishMapper.updateById(finish));
            inventoryRecorder.receipt(finish, order.getUuid(), "DISPOSITION:" + dto.getRequestId(), now);
        }
        roll.setActualWeight(dto.getActualWeight());
        roll.setProcessMode(PROCESS_MODE_DIRECT_SHIP);
        roll.setMainStepType(null);
        roll.setWeightStatus(WeightStatus.MEASURED.name());
        roll.setWeightSource("SCALE");
        roll.setWeightRecordedAt(now);
        roll.setWeightRecordedBy(currentOperator());
        markDisposed(roll, ProcessRollDispositionAction.DIRECT_SHIP);
        orderService.calcFee(order.getUuid());
        return base(order, roll, dto.getAction(), direct.finishes());
    }

    private ProcessRollDisposition applySplit(ProcessOrder order, OriginalRoll roll,
                                              ProcessRollDispositionDTO dto, LocalDateTime now) {
        routeCleanupService.clearExistingRoute(new ProcessRouteContext(order, roll));
        ProcessOrderCreateDTO create = ProcessRollSplitFactory.orderRequest(order, roll);
        String targetUuid = orderService.create(create);
        OriginalRoll target = firstTargetRoll(targetUuid);
        markDisposed(roll, ProcessRollDispositionAction.SPLIT_TO_ORDER);
        orderService.calcFee(order.getUuid());
        ProcessRollDisposition result = base(order, roll, dto.getAction());
        result.setTargetOrderUuid(targetUuid);
        result.setTargetRollUuid(target.getUuid());
        return result;
    }

    private OriginalRoll copyForDirectShip(OriginalRoll roll, java.math.BigDecimal weight) {
        OriginalRoll copy = new OriginalRoll();
        BeanUtils.copyProperties(roll, copy);
        copy.setActualWeight(weight);
        copy.setProcessMode(3);
        copy.setIsChecked(0);
        return copy;
    }

    private void requireNoDownstreamReference(ProcessOrder order, OriginalRoll roll) {
        long settled = settleDetailMapper.selectCount(new LambdaQueryWrapper<SettleDetail>()
                .eq(SettleDetail::getOrderUuid, order.getUuid())
                .eq(SettleDetail::getIsDeleted, 0));
        if (settled > 0) throw new BusinessException("加工单已进入结算，不能处置母卷");
        List<String> finishIds = relationMapper.selectList(new LambdaQueryWrapper<FinishOriginalRel>()
                        .eq(FinishOriginalRel::getOrderUuid, order.getUuid())
                        .eq(FinishOriginalRel::getOriginalUuid, roll.getUuid()))
                .stream().map(FinishOriginalRel::getFinishUuid).toList();
        if (finishIds.isEmpty()) return;
        long delivered = deliveryDetailMapper.selectCount(new LambdaQueryWrapper<DeliveryDetail>()
                .in(DeliveryDetail::getFinishUuid, finishIds)
                .eq(DeliveryDetail::getIsDeleted, 0));
        if (delivered > 0) throw new BusinessException("母卷成品已被出库单引用，不能处置");
        List<FinishRoll> finishes = finishMapper.selectBatchIds(finishIds);
        if (finishes.stream().anyMatch(f -> f.getActualWeight() != null && f.getActualWeight().signum() > 0)) {
            throw new BusinessException("母卷已有实际产出，不能直接处置");
        }
    }

    private String resolveWarehouse(ProcessOrder order, ProcessRollDispositionDTO dto) {
        String current = order.getWarehouseUuid();
        if (StringUtils.hasText(current) && StringUtils.hasText(dto.getWarehouseUuid())
                && !current.equals(dto.getWarehouseUuid().trim())) {
            throw new BusinessException("直发仓库必须与加工单已确定仓库一致");
        }
        String warehouse = StringUtils.hasText(current)
                ? current.trim()
                : StringUtils.hasText(dto.getWarehouseUuid()) ? dto.getWarehouseUuid().trim() : null;
        if (dto.getAction() != ProcessRollDispositionAction.DIRECT_SHIP) return warehouse;
        return backRecordWarehousePolicy.requireEnabled(warehouse).uuid();
    }

    private void markDisposed(OriginalRoll roll, ProcessRollDispositionAction action) {
        roll.setIsChecked(CHECKED);
        roll.setDispositionAction(action);
        if (action == ProcessRollDispositionAction.DIRECT_SHIP) {
            roll.setRollStatus(ROLL_DIRECT);
        }
        ConcurrencyGuard.requireRowUpdated(rollMapper.updateById(roll));
    }

    private ProcessRollDisposition base(ProcessOrder order, OriginalRoll roll,
                                        ProcessRollDispositionAction action) {
        return base(order, roll, action, List.of());
    }

    private ProcessRollDisposition base(ProcessOrder order, OriginalRoll roll,
                                        ProcessRollDispositionAction action, List<FinishRoll> targetFinishes) {
        ProcessRollDisposition result = new ProcessRollDisposition();
        result.setSourceOrderUuid(order.getUuid());
        result.setSourceRollUuid(roll.getUuid());
        result.setActionType(action);
        result.setStatus(ProcessRollDispositionStatus.APPLIED);
        List<String> targetUuids = targetFinishes.stream().map(FinishRoll::getUuid).toList();
        result.setTargetFinishUuid(targetUuids.isEmpty() ? null : targetUuids.get(0));
        result.setTargetFinishUuids(serializeTargetFinishUuids(targetUuids));
        return result;
    }

    private String serializeTargetFinishUuids(List<String> targetUuids) {
        if (targetUuids.isEmpty()) return null;
        try {
            return JSON.writeValueAsString(targetUuids);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("直发成品审计信息生成失败");
        }
    }

    private ProcessRollDisposition findByRoll(String rollUuid) {
        return dispositionMapper.selectOne(new LambdaQueryWrapper<ProcessRollDisposition>()
                .eq(ProcessRollDisposition::getSourceRollUuid, rollUuid)
                .eq(ProcessRollDisposition::getStatus, ProcessRollDispositionStatus.APPLIED)
                .eq(ProcessRollDisposition::getIsDeleted, 0));
    }

    private void ensureRequestFree(String requestId, String rollUuid) {
        ProcessRollDisposition found = dispositionMapper.selectOne(new LambdaQueryWrapper<ProcessRollDisposition>()
                .eq(ProcessRollDisposition::getRequestId, requestId.trim())
                .eq(ProcessRollDisposition::getIsDeleted, 0));
        if (found != null && !rollUuid.equals(found.getSourceRollUuid())) {
            throw new BusinessException("幂等请求号已用于其他母卷");
        }
    }

    private OriginalRoll requireRoll(String rollUuid) {
        OriginalRoll roll = rollMapper.selectById(rollUuid);
        if (roll == null) throw new BusinessException("母卷不存在");
        return roll;
    }

    private ProcessOrder requireOrder(String uuid) {
        ProcessOrder order = orderService.getById(uuid);
        if (order == null) throw new BusinessException("加工单不存在");
        return order;
    }

    private void requireVersion(ProcessOrder order, Integer expected) {
        if (!expected.equals(order.getVersion())) throw new BusinessException("加工单已发生变化，请刷新后重试");
    }

    private OriginalRoll firstTargetRoll(String orderUuid) {
        OriginalRoll target = rollMapper.selectOne(new LambdaQueryWrapper<OriginalRoll>()
                .eq(OriginalRoll::getOrderUuid, orderUuid).orderByAsc(OriginalRoll::getRowSort).last("LIMIT 1"));
        if (target == null) throw new BusinessException("拆分代加工单未生成母卷");
        return target;
    }

    private ProcessOrder touchOrder(String orderUuid) {
        ProcessOrder current = orderService.getById(orderUuid);
        if (current == null) throw new BusinessException("加工单不存在");
        current.setUpdateBy(currentOperator());
        ConcurrencyGuard.requireUpdated(orderService.updateById(current));
        return orderService.getById(orderUuid);
    }

    private String currentOperator() {
        return com.paper.mes.auth.context.AuthContextHolder.currentDisplayName();
    }

    private String rollLabel(OriginalRoll roll) {
        return StringUtils.hasText(roll.getRollNo()) ? roll.getRollNo() : roll.getUuid();
    }

    private ProcessRollDispositionVO result(ProcessRollDisposition item) {
        ProcessRollDispositionVO vo = new ProcessRollDispositionVO();
        vo.setSourceOrderUuid(item.getSourceOrderUuid());
        vo.setSourceRollUuid(item.getSourceRollUuid());
        vo.setAction(item.getActionType());
        vo.setTargetOrderUuid(item.getTargetOrderUuid());
        vo.setTargetRollUuid(item.getTargetRollUuid());
        vo.setTargetFinishUuid(item.getTargetFinishUuid());
        vo.setTargetFinishUuids(parseTargetFinishUuids(item));
        vo.setOperatedAt(item.getOperateTime());
        ProcessOrder order = orderService.getById(item.getSourceOrderUuid());
        if (order != null) vo.setSourceOrderNo(order.getOrderNo());
        if (StringUtils.hasText(item.getTargetOrderUuid())) {
            ProcessOrder target = orderService.getById(item.getTargetOrderUuid());
            if (target != null) vo.setTargetOrderNo(target.getOrderNo());
        }
        return vo;
    }

    private List<String> parseTargetFinishUuids(ProcessRollDisposition item) {
        if (StringUtils.hasText(item.getTargetFinishUuids())) {
            try {
                return JSON.readValue(item.getTargetFinishUuids(), new TypeReference<>() { });
            } catch (JsonProcessingException ignored) {
                // Legacy or malformed rows retain the first UUID below for compatibility.
            }
        }
        return StringUtils.hasText(item.getTargetFinishUuid())
                ? List.of(item.getTargetFinishUuid()) : List.of();
    }
}

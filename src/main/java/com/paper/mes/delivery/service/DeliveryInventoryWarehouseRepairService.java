package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.PageRequestBounds;
import com.paper.mes.common.PageResult;
import com.paper.mes.delivery.dto.DeliveryInventoryUnassignedOrderVO;
import com.paper.mes.delivery.dto.DeliveryInventoryUnassignedQuery;
import com.paper.mes.delivery.dto.DeliveryInventoryWarehouseRepairRequest;
import com.paper.mes.delivery.dto.DeliveryInventoryWarehouseRepairResultVO;
import com.paper.mes.delivery.mapper.DeliveryInventoryWarehouseRepairMapper;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.warehouse.entity.Warehouse;
import com.paper.mes.warehouse.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryInventoryWarehouseRepairService {

    private static final int ENABLED = 1;
    private static final Set<Integer> REPAIRABLE_ORDER_STATUS = Set.of(4, 5);
    private static final Set<Integer> REPAIRABLE_FINISH_STATUS = Set.of(2, 3);

    private final DeliveryInventoryWarehouseRepairMapper mapper;
    private final WarehouseMapper warehouseMapper;
    private final BusinessLockService businessLockService;
    private final OperationLogService operationLogService;
    private final DeliveryInventorySnapshotWarehousePatcher snapshotPatcher;

    public PageResult<DeliveryInventoryUnassignedOrderVO> page(DeliveryInventoryUnassignedQuery query) {
        Page<DeliveryInventoryUnassignedOrderVO> page = PageRequestBounds.of(query.getCurrent(), query.getSize());
        page.setTotal(mapper.countUnassignedOrders(query));
        page.setRecords(mapper.selectUnassignedOrders(query, page.offset(), page.getSize()));
        return PageResult.of(page);
    }

    @Transactional(rollbackFor = Exception.class)
    public DeliveryInventoryWarehouseRepairResultVO assign(DeliveryInventoryWarehouseRepairRequest request) {
        List<String> orderUuids = normalizedOrderUuids(request);
        Warehouse warehouse = requireEnabledWarehouse(request.getWarehouseUuid());
        businessLockService.lockProcessOrders(orderUuids);
        List<ProcessOrder> orders = mapper.selectOrdersForRepair(orderUuids);
        requireAllOrdersPresent(orderUuids, orders);
        orders.forEach(this::requireRepairableOrder);
        List<FinishRoll> beforeLock = mapper.selectFinishesForRepair(orderUuids);
        businessLockService.lockFinishRolls(beforeLock.stream().map(FinishRoll::getUuid).sorted().toList());
        List<FinishRoll> finishes = mapper.selectFinishesForRepair(orderUuids);
        Map<String, ProcessOrder> orderByUuid = orders.stream().collect(Collectors.toMap(ProcessOrder::getUuid, Function.identity()));
        List<FinishRoll> targets = validateTargets(finishes, orderByUuid, warehouse.getUuid());
        if (mapper.countActiveLocks(targets.stream().map(FinishRoll::getUuid).toList()) > 0) {
            throw new BusinessException(ErrorCode.E004, "存在活动出库占用，暂不能补录仓库");
        }
        updateOrdersAndSnapshot(orders, targets, warehouse, request.getReason());
        int updated = mapper.assignFinishWarehouse(targets.stream().map(FinishRoll::getUuid).toList(), warehouse.getUuid(), operator());
        ConcurrencyGuard.requireRowUpdated(updated == targets.size() ? updated : 0);
        return new DeliveryInventoryWarehouseRepairResultVO(orders.size(), targets.size(), warehouse.getUuid(), warehouse.getWarehouseName());
    }

    private List<String> normalizedOrderUuids(DeliveryInventoryWarehouseRepairRequest request) {
        List<String> ids = request.getOrderUuids().stream().filter(StringUtils::hasText).map(String::trim).distinct().sorted().toList();
        if (ids.isEmpty()) throw new BusinessException("请选择需要补仓的加工单");
        return ids;
    }

    private Warehouse requireEnabledWarehouse(String uuid) {
        Warehouse warehouse = warehouseMapper.selectById(uuid);
        if (warehouse == null || !Objects.equals(ENABLED, warehouse.getStatus())) {
            throw new BusinessException(ErrorCode.E002, "补录仓库不存在或已停用");
        }
        return warehouse;
    }

    private void requireAllOrdersPresent(List<String> ids, List<ProcessOrder> orders) {
        if (orders.size() != ids.size()) throw new BusinessException(ErrorCode.E002, "部分加工单不存在或已删除");
    }

    private void requireRepairableOrder(ProcessOrder order) {
        if (!REPAIRABLE_ORDER_STATUS.contains(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.E001, "仅已完成或已结算加工单允许历史补仓：" + order.getOrderNo());
        }
    }

    private List<FinishRoll> validateTargets(List<FinishRoll> finishes, Map<String, ProcessOrder> orders, String warehouseUuid) {
        Map<String, List<FinishRoll>> byOrder = finishes.stream().collect(Collectors.groupingBy(FinishRoll::getOrderUuid));
        List<FinishRoll> targets = finishes.stream().filter(this::isTarget).toList();
        for (ProcessOrder order : orders.values()) validateOrderWarehouse(order, byOrder.getOrDefault(order.getUuid(), List.of()), warehouseUuid, targets);
        if (targets.isEmpty()) throw new BusinessException("所选加工单没有可补录的历史空仓成品");
        return targets;
    }

    private void validateOrderWarehouse(ProcessOrder order, List<FinishRoll> finishes, String warehouseUuid, List<FinishRoll> targets) {
        Set<String> known = finishes.stream().filter(this::isRepairableFinish).map(FinishRoll::getWarehouseUuid)
                .filter(StringUtils::hasText).collect(Collectors.toSet());
        if (StringUtils.hasText(order.getWarehouseUuid())) known.add(order.getWarehouseUuid());
        if (known.size() > 1 || (known.size() == 1 && !known.contains(warehouseUuid))) {
            throw new BusinessException("加工单已有其他仓库归属，请选择一致仓库：" + order.getOrderNo());
        }
        if (targets.stream().noneMatch(finish -> order.getUuid().equals(finish.getOrderUuid()))) {
            throw new BusinessException("加工单没有可补录的历史空仓成品：" + order.getOrderNo());
        }
    }

    private boolean isTarget(FinishRoll finish) {
        return isRepairableFinish(finish) && !StringUtils.hasText(finish.getWarehouseUuid());
    }

    private boolean isRepairableFinish(FinishRoll finish) {
        return REPAIRABLE_FINISH_STATUS.contains(finish.getFinishStatus());
    }

    private void updateOrdersAndSnapshot(List<ProcessOrder> orders, List<FinishRoll> targets,
                                         Warehouse warehouse, String reason) {
        Map<String, Set<String>> targetIds = targets.stream().collect(Collectors.groupingBy(
                FinishRoll::getOrderUuid, Collectors.mapping(FinishRoll::getUuid, Collectors.toSet())));
        for (ProcessOrder order : orders) {
            Set<String> ids = targetIds.getOrDefault(order.getUuid(), Set.of());
            String snap = snapshotPatcher.patch(order.getSnapFinish(), warehouse, ids);
            int updated = mapper.assignOrderWarehouse(order.getUuid(), warehouse.getUuid(), snap, operator());
            ConcurrencyGuard.requireRowUpdated(updated);
            operationLogService.record(OperationLogService.BIZ_TYPE_ORDER, order.getUuid(), order.getOrderNo(),
                    OperationLogService.ACTION_DATA_REPAIR, operator(), "历史库存补录仓库：" + warehouse.getWarehouseName() + "；原因：" + reason.trim());
        }
    }

    private String operator() {
        return AuthContextHolder.currentDisplayName();
    }
}

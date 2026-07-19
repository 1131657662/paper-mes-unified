package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.warehouse.entity.Warehouse;
import com.paper.mes.warehouse.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DeliveryWarehousePolicy {

    private static final int WAREHOUSE_ENABLED = 1;

    private final WarehouseMapper warehouseMapper;

    public WarehouseSnapshot requireForCreate(String warehouseUuid, List<FinishRoll> finishes) {
        Warehouse warehouse = requireEnabledWarehouse(warehouseUuid);
        validateFinishes(warehouseUuid, finishes);
        return snapshot(warehouse);
    }

    public WarehouseSnapshot requireForAppend(DeliveryOrder order, List<FinishRoll> existing,
                                              List<FinishRoll> appended) {
        String warehouseUuid = StringUtils.hasText(order.getWarehouseUuid())
                ? order.getWarehouseUuid() : resolveCommonWarehouse(existing, appended);
        Warehouse warehouse = requireEnabledWarehouse(warehouseUuid);
        validateFinishes(warehouseUuid, appended);
        if (!StringUtils.hasText(order.getWarehouseUuid())) {
            validateFinishes(warehouseUuid, existing);
        }
        return snapshot(warehouse);
    }

    private String resolveCommonWarehouse(List<FinishRoll> existing, List<FinishRoll> appended) {
        List<String> warehouseUuids = java.util.stream.Stream.concat(existing.stream(), appended.stream())
                .map(FinishRoll::getWarehouseUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        boolean hasUnassigned = java.util.stream.Stream.concat(existing.stream(), appended.stream())
                .anyMatch(finish -> !StringUtils.hasText(finish.getWarehouseUuid()));
        if (hasUnassigned || warehouseUuids.size() != 1) {
            throw new BusinessException("历史出库单无法确定唯一仓库，请重新创建出库单");
        }
        return warehouseUuids.getFirst();
    }

    private Warehouse requireEnabledWarehouse(String warehouseUuid) {
        if (!StringUtils.hasText(warehouseUuid)) {
            throw new BusinessException("出库仓库不能为空");
        }
        Warehouse warehouse = warehouseMapper.selectById(warehouseUuid);
        if (warehouse == null || !Objects.equals(WAREHOUSE_ENABLED, warehouse.getStatus())) {
            throw new BusinessException(ErrorCode.E002, "出库仓库不存在或已停用");
        }
        return warehouse;
    }

    private void validateFinishes(String warehouseUuid, List<FinishRoll> finishes) {
        FinishRoll mismatch = finishes.stream()
                .filter(finish -> !warehouseUuid.equals(finish.getWarehouseUuid()))
                .findFirst()
                .orElse(null);
        if (mismatch != null) {
            throw new BusinessException("成品不属于所选仓库：" + mismatch.getFinishRollNo());
        }
    }

    private WarehouseSnapshot snapshot(Warehouse warehouse) {
        return new WarehouseSnapshot(warehouse.getUuid(), warehouse.getWarehouseName(), warehouse.getLocation());
    }

    public record WarehouseSnapshot(String uuid, String name, String location) {
    }
}

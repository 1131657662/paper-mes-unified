package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.warehouse.entity.Warehouse;
import com.paper.mes.warehouse.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class BackRecordWarehousePolicy {

    private static final int STATUS_ENABLED = 1;

    private final WarehouseMapper warehouseMapper;

    public WarehouseSnapshot requireEnabled(String warehouseUuid) {
        if (!StringUtils.hasText(warehouseUuid)) {
            throw new BusinessException("请选择入库仓库");
        }
        Warehouse warehouse = warehouseMapper.selectById(warehouseUuid);
        if (warehouse == null) {
            throw new BusinessException("入库仓库不存在或已删除");
        }
        if (!Integer.valueOf(STATUS_ENABLED).equals(warehouse.getStatus())) {
            throw new BusinessException("入库仓库已停用，请重新选择");
        }
        return new WarehouseSnapshot(
                warehouse.getUuid(), warehouse.getWarehouseName(), warehouse.getLocation());
    }

    public record WarehouseSnapshot(String uuid, String name, String location) {
    }
}

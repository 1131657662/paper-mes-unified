package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.warehouse.entity.Warehouse;
import com.paper.mes.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeliveryWarehousePolicyTest {

    private final WarehouseMapper warehouseMapper = mock(WarehouseMapper.class);
    private final DeliveryWarehousePolicy policy = new DeliveryWarehousePolicy(warehouseMapper);

    @Test
    void requireForCreate_withOneEnabledWarehouse_returnsSnapshot() {
        when(warehouseMapper.selectById("warehouse-1")).thenReturn(warehouse("warehouse-1", 1));

        DeliveryWarehousePolicy.WarehouseSnapshot result = policy.requireForCreate(
                "warehouse-1", List.of(finish("P000001", "warehouse-1")));

        assertEquals("warehouse-1", result.uuid());
        assertEquals("成品一仓", result.name());
    }

    @Test
    void requireForCreate_withCrossWarehouseFinish_rejectsRequest() {
        when(warehouseMapper.selectById("warehouse-1")).thenReturn(warehouse("warehouse-1", 1));

        assertThrows(BusinessException.class, () -> policy.requireForCreate(
                "warehouse-1", List.of(finish("P000002", "warehouse-2"))));
    }

    @Test
    void requireForAppend_forLegacyOrder_infersSingleWarehouse() {
        when(warehouseMapper.selectById("warehouse-1")).thenReturn(warehouse("warehouse-1", 1));
        DeliveryOrder order = new DeliveryOrder();

        DeliveryWarehousePolicy.WarehouseSnapshot result = policy.requireForAppend(order,
                List.of(finish("P000001", "warehouse-1")),
                List.of(finish("P000002", "warehouse-1")));

        assertEquals("warehouse-1", result.uuid());
    }

    @Test
    void requireForAppend_forLegacyCrossWarehouseOrder_rejectsRequest() {
        DeliveryOrder order = new DeliveryOrder();

        assertThrows(BusinessException.class, () -> policy.requireForAppend(order,
                List.of(finish("P000001", "warehouse-1")),
                List.of(finish("P000002", "warehouse-2"))));
    }

    private Warehouse warehouse(String uuid, int status) {
        Warehouse warehouse = new Warehouse();
        warehouse.setUuid(uuid);
        warehouse.setWarehouseName("成品一仓");
        warehouse.setLocation("一号库区");
        warehouse.setStatus(status);
        return warehouse;
    }

    private FinishRoll finish(String rollNo, String warehouseUuid) {
        FinishRoll finish = new FinishRoll();
        finish.setFinishRollNo(rollNo);
        finish.setWarehouseUuid(warehouseUuid);
        return finish;
    }
}

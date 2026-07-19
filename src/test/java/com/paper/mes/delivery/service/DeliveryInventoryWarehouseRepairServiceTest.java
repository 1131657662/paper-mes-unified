package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.delivery.dto.DeliveryInventoryWarehouseRepairRequest;
import com.paper.mes.delivery.mapper.DeliveryInventoryWarehouseRepairMapper;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.warehouse.entity.Warehouse;
import com.paper.mes.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryInventoryWarehouseRepairServiceTest {

    @Mock private DeliveryInventoryWarehouseRepairMapper mapper;
    @Mock private WarehouseMapper warehouseMapper;
    @Mock private BusinessLockService businessLockService;
    @Mock private OperationLogService operationLogService;
    @Mock private DeliveryInventorySnapshotWarehousePatcher snapshotPatcher;

    private DeliveryInventoryWarehouseRepairService service;

    @BeforeEach
    void setUp() {
        service = new DeliveryInventoryWarehouseRepairService(mapper, warehouseMapper, businessLockService,
                operationLogService, snapshotPatcher);
    }

    @Test
    void assign_withHistoricalUnassignedRoll_updatesOrderRollAndAudit() {
        Warehouse warehouse = warehouse("warehouse-1");
        ProcessOrder order = order("order-1", null);
        FinishRoll finish = finish("finish-1", "order-1", null);
        when(warehouseMapper.selectById("warehouse-1")).thenReturn(warehouse);
        when(mapper.selectOrdersForRepair(List.of("order-1"))).thenReturn(List.of(order));
        when(mapper.selectFinishesForRepair(List.of("order-1"))).thenReturn(List.of(finish));
        when(mapper.countActiveLocks(List.of("finish-1"))).thenReturn(0L);
        when(snapshotPatcher.patch("{}", warehouse, java.util.Set.of("finish-1"))).thenReturn("{}");
        when(mapper.assignOrderWarehouse(eq("order-1"), eq("warehouse-1"), eq("{}"), any())).thenReturn(1);
        when(mapper.assignFinishWarehouse(List.of("finish-1"), "warehouse-1", "system")).thenReturn(1);

        var result = service.assign(request("order-1", "warehouse-1", "历史数据补录仓库"));

        assertEquals(1, result.getRepairedOrderCount());
        assertEquals(1, result.getRepairedRollCount());
        verify(businessLockService).lockProcessOrders(List.of("order-1"));
        verify(businessLockService).lockFinishRolls(List.of("finish-1"));
        verify(operationLogService).record(eq(OperationLogService.BIZ_TYPE_ORDER), eq("order-1"),
                eq("JG-order-1"), eq(OperationLogService.ACTION_DATA_REPAIR), any(), any());
    }

    @Test
    void assign_withDisabledWarehouse_rejectsBeforeLocking() {
        Warehouse warehouse = warehouse("warehouse-1");
        warehouse.setStatus(2);
        when(warehouseMapper.selectById("warehouse-1")).thenReturn(warehouse);

        assertThrows(BusinessException.class,
                () -> service.assign(request("order-1", "warehouse-1", "历史数据补录仓库")));

        verify(businessLockService, never()).lockProcessOrders(anyList());
        verify(mapper, never()).assignFinishWarehouse(anyList(), any(), any());
    }

    @Test
    void assign_withExistingDifferentWarehouse_rejectsWithoutUpdating() {
        Warehouse warehouse = warehouse("warehouse-2");
        ProcessOrder order = order("order-1", "warehouse-1");
        FinishRoll finish = finish("finish-1", "order-1", null);
        when(warehouseMapper.selectById("warehouse-2")).thenReturn(warehouse);
        when(mapper.selectOrdersForRepair(List.of("order-1"))).thenReturn(List.of(order));
        when(mapper.selectFinishesForRepair(List.of("order-1"))).thenReturn(List.of(finish));

        assertThrows(BusinessException.class,
                () -> service.assign(request("order-1", "warehouse-2", "历史数据补录仓库")));

        verify(mapper, never()).assignOrderWarehouse(any(), any(), any(), any());
        verify(mapper, never()).assignFinishWarehouse(anyList(), any(), any());
    }

    @Test
    void assign_withActiveDeliveryLock_rejectsBeforeUpdating() {
        Warehouse warehouse = warehouse("warehouse-1");
        ProcessOrder order = order("order-1", null);
        FinishRoll finish = finish("finish-1", "order-1", null);
        when(warehouseMapper.selectById("warehouse-1")).thenReturn(warehouse);
        when(mapper.selectOrdersForRepair(List.of("order-1"))).thenReturn(List.of(order));
        when(mapper.selectFinishesForRepair(List.of("order-1"))).thenReturn(List.of(finish));
        when(mapper.countActiveLocks(List.of("finish-1"))).thenReturn(1L);

        assertThrows(BusinessException.class,
                () -> service.assign(request("order-1", "warehouse-1", "历史数据补录仓库")));

        verify(mapper, never()).assignOrderWarehouse(any(), any(), any(), any());
        verify(mapper, never()).assignFinishWarehouse(anyList(), any(), any());
    }

    private DeliveryInventoryWarehouseRepairRequest request(String orderUuid, String warehouseUuid, String reason) {
        DeliveryInventoryWarehouseRepairRequest request = new DeliveryInventoryWarehouseRepairRequest();
        request.setOrderUuids(List.of(orderUuid));
        request.setWarehouseUuid(warehouseUuid);
        request.setReason(reason);
        return request;
    }

    private Warehouse warehouse(String uuid) {
        Warehouse warehouse = new Warehouse();
        warehouse.setUuid(uuid);
        warehouse.setWarehouseName("演示成品仓");
        warehouse.setStatus(1);
        return warehouse;
    }

    private ProcessOrder order(String uuid, String warehouseUuid) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid(uuid);
        order.setOrderNo("JG-" + uuid);
        order.setOrderStatus(4);
        order.setWarehouseUuid(warehouseUuid);
        order.setSnapFinish("{}");
        return order;
    }

    private FinishRoll finish(String uuid, String orderUuid, String warehouseUuid) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setOrderUuid(orderUuid);
        finish.setFinishRollNo("P-" + uuid);
        finish.setFinishStatus(2);
        finish.setWarehouseUuid(warehouseUuid);
        finish.setRemainingWeight(BigDecimal.ZERO);
        return finish;
    }
}

package com.paper.mes.delivery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.warehouse.entity.Warehouse;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeliveryInventorySnapshotWarehousePatcherTest {

    @Test
    void patch_addsWarehouseToLegacySnapshotAndMatchingFinishRows() throws Exception {
        Warehouse warehouse = new Warehouse();
        warehouse.setUuid("warehouse-1");
        warehouse.setWarehouseName("演示成品仓");
        warehouse.setLocation("A区");
        DeliveryInventorySnapshotWarehousePatcher patcher =
                new DeliveryInventorySnapshotWarehousePatcher(new ObjectMapper());

        String result = patcher.patch("{\"finish_rolls\":[{\"uuid\":\"finish-1\"},{\"uuid\":\"finish-2\"}]}",
                warehouse, Set.of("finish-1"));

        var root = new ObjectMapper().readTree(result);
        assertEquals("warehouse-1", root.path("warehouse").path("uuid").asText());
        assertEquals("warehouse-1", root.path("finish_rolls").get(0).path("warehouse_uuid").asText());
        assertEquals("", root.path("finish_rolls").get(1).path("warehouse_uuid").asText());
    }

    @Test
    void patch_withDifferentSnapshotWarehouse_rejectsOverwrite() {
        Warehouse warehouse = new Warehouse();
        warehouse.setUuid("warehouse-2");
        warehouse.setWarehouseName("二号仓");
        DeliveryInventorySnapshotWarehousePatcher patcher =
                new DeliveryInventorySnapshotWarehousePatcher(new ObjectMapper());

        assertThrows(BusinessException.class, () -> patcher.patch(
                "{\"warehouse\":{\"uuid\":\"warehouse-1\"}}", warehouse, Set.of()));
    }
}

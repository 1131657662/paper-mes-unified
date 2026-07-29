package com.paper.mes.integration;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.delivery.dto.DeliveryInventoryUnassignedOrderVO;
import com.paper.mes.delivery.dto.DeliveryInventoryUnassignedQuery;
import com.paper.mes.delivery.mapper.DeliveryInventoryWarehouseRepairMapper;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.warehouse.entity.Warehouse;
import com.paper.mes.warehouse.mapper.WarehouseMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class DeliveryInventoryWarehouseRepairBusinessFlowIT extends AuthenticatedBusinessFlowIT {

    @Autowired private BusinessFlowFixtureFactory fixtures;
    @Autowired private DeliveryInventoryWarehouseRepairMapper repairMapper;
    @Autowired private FinishRollMapper finishRollMapper;
    @Autowired private WarehouseMapper warehouseMapper;

    @Test
    void unassignedOrder_whenKnownWarehousesConflict_hidesAmbiguousWarehouseIdentity() {
        BusinessFlowFixtureFactory.Scenario scenario = fixtures.createCompletedOrderWithTwoFinishes();
        Warehouse conflictingWarehouse = warehouse();
        warehouseMapper.insert(conflictingWarehouse);
        finishRollMapper.update(null, new LambdaUpdateWrapper<FinishRoll>()
                .eq(FinishRoll::getUuid, scenario.first().getUuid())
                .set(FinishRoll::getWarehouseUuid, null));
        finishRollMapper.update(null, new LambdaUpdateWrapper<FinishRoll>()
                .eq(FinishRoll::getUuid, scenario.second().getUuid())
                .set(FinishRoll::getWarehouseUuid, conflictingWarehouse.getUuid()));
        DeliveryInventoryUnassignedQuery query = new DeliveryInventoryUnassignedQuery();
        query.setKeyword(scenario.order().getOrderNo());

        List<DeliveryInventoryUnassignedOrderVO> rows = repairMapper.selectUnassignedOrders(query, 0, 20);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().isWarehouseConflict()).isTrue();
        assertThat(rows.getFirst().getKnownWarehouseUuid()).isNull();
        assertThat(rows.getFirst().getKnownWarehouseName()).isNull();
    }

    private Warehouse warehouse() {
        String token = UUID.randomUUID().toString().replace("-", "");
        Warehouse warehouse = new Warehouse();
        warehouse.setUuid(token);
        warehouse.setWarehouseCode("IT-WH-C-" + token.substring(0, 8));
        warehouse.setWarehouseName("Inventory conflict warehouse");
        warehouse.setStatus(1);
        warehouse.setIsDefault(0);
        return warehouse;
    }
}

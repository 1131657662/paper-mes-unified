package com.paper.mes.processorder.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishRoll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinishRollServiceImplTest {

    @Test
    void changeFinishStatus_whenPendingInToScrap_updatesStatus() {
        FinishRoll roll = roll(1);
        FinishRollServiceImpl service = serviceReturning(roll);

        service.changeFinishStatus("finish-1", 4);

        assertEquals(4, roll.getFinishStatus());
    }

    @Test
    void changeFinishStatus_whenPendingInToInStock_rejectsManualInventoryChange() {
        FinishRollServiceImpl service = serviceReturning(roll(1));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.changeFinishStatus("finish-1", 2));

        assertTrue(error.getMessage().contains("加工回录"));
    }

    @Test
    void changeFinishStatus_whenInStockToOutStock_rejectsManualDeliveryChange() {
        FinishRollServiceImpl service = serviceReturning(roll(2));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.changeFinishStatus("finish-1", 3));

        assertTrue(error.getMessage().contains("出库单确认"));
    }

    private FinishRoll roll(int finishStatus) {
        FinishRoll roll = new FinishRoll();
        roll.setUuid("finish-1");
        roll.setFinishRollNo("A000001");
        roll.setFinishStatus(finishStatus);
        return roll;
    }

    private FinishRollServiceImpl serviceReturning(FinishRoll roll) {
        return new FinishRollServiceImpl(null, null, null, null) {
            @Override
            public FinishRoll getById(Serializable id) {
                return roll;
            }

            @Override
            public boolean updateById(FinishRoll entity) {
                return true;
            }
        };
    }
}

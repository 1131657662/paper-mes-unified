package com.paper.mes.warehouse.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WarehouseInventoryGuardTest {

    private FinishRollMapper finishRollMapper;
    private WarehouseInventoryGuard guard;

    @BeforeEach
    void setUp() {
        finishRollMapper = mock(FinishRollMapper.class);
        guard = new WarehouseInventoryGuard(finishRollMapper);
    }

    @Test
    void requireNoActiveInventory_whenWarehouseHasNoStock_allowsArchive() {
        assertDoesNotThrow(() -> guard.requireNoActiveInventory("warehouse-1"));
    }

    @Test
    void requireNoActiveInventory_whenWarehouseHasStock_rejectsArchive() {
        when(finishRollMapper.selectOne(any())).thenReturn(new FinishRoll());

        assertThrows(BusinessException.class, () -> guard.requireNoActiveInventory("warehouse-1"));
    }
}

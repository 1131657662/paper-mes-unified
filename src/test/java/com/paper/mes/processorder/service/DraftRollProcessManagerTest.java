package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.dto.DraftRollProcessBatchSaveDTO;
import com.paper.mes.processorder.dto.DraftRollProcessDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DraftRollProcessManagerTest {

    private ProcessOrderMapper orderMapper;
    private OriginalRollMapper rollMapper;
    private ProcessConfigDraftMapper draftMapper;
    private DraftOrderVersionGuard versionGuard;
    private DraftRollProcessManager manager;

    @BeforeEach
    void setUp() {
        orderMapper = mock(ProcessOrderMapper.class);
        rollMapper = mock(OriginalRollMapper.class);
        draftMapper = mock(ProcessConfigDraftMapper.class);
        versionGuard = mock(DraftOrderVersionGuard.class);
        manager = new DraftRollProcessManager(
                orderMapper, rollMapper, draftMapper, mock(BusinessLockService.class), versionGuard);
    }

    @Test
    void save_twoRolls_advancesDraftVersionOnce() {
        when(orderMapper.selectById("order-1")).thenReturn(order());
        when(rollMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                roll("roll-1"), roll("roll-2")));
        when(rollMapper.updateById(any(OriginalRoll.class))).thenReturn(1);

        manager.save("order-1", request("roll-1", "roll-2"));

        verify(versionGuard).assertExpected(any(ProcessOrder.class), org.mockito.ArgumentMatchers.eq(7));
        verify(versionGuard).advance("order-1", 7);
        verify(rollMapper, times(2)).updateById(any(OriginalRoll.class));
    }

    @Test
    void save_duplicateRoll_rejectsBeforeAdvancingVersion() {
        when(orderMapper.selectById("order-1")).thenReturn(order());

        assertThatThrownBy(() -> manager.save("order-1", request("roll-1", "roll-1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重复");

        verify(versionGuard, never()).advance(any(), any());
        verify(rollMapper, never()).selectBatchIds(anyCollection());
    }

    @Test
    void save_withChangedProcess_removesStalePlanDraft() {
        OriginalRoll roll = roll("roll-1");
        roll.setProcessMode(2);
        roll.setMainStepType(2);
        when(orderMapper.selectById("order-1")).thenReturn(order());
        when(rollMapper.selectBatchIds(anyCollection())).thenReturn(List.of(roll));
        when(rollMapper.updateById(any(OriginalRoll.class))).thenReturn(1);

        manager.save("order-1", request("roll-1"));

        verify(draftMapper).delete(any());
    }

    private DraftRollProcessBatchSaveDTO request(String... ids) {
        DraftRollProcessBatchSaveDTO dto = new DraftRollProcessBatchSaveDTO();
        dto.setExpectedVersion(7);
        dto.setRolls(java.util.Arrays.stream(ids).map(this::item).toList());
        return dto;
    }

    private DraftRollProcessDTO item(String id) {
        DraftRollProcessDTO item = new DraftRollProcessDTO();
        item.setOriginalUuid(id);
        item.setProcessMode(1);
        item.setMainStepType(1);
        return item;
    }

    private ProcessOrder order() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(0);
        order.setVersion(7);
        return order;
    }

    private OriginalRoll roll(String uuid) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setOrderUuid("order-1");
        roll.setVersion(1);
        return roll;
    }
}

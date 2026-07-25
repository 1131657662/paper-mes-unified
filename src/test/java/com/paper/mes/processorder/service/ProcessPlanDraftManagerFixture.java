package com.paper.mes.processorder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.dto.PlanPreviewVO;
import com.paper.mes.processorder.dto.ProcessPlanBatchItemDTO;
import com.paper.mes.processorder.dto.ProcessPlanBatchSaveDTO;
import com.paper.mes.processorder.dto.ProcessPlanDTO;
import com.paper.mes.processorder.dto.ProcessPlanItemsBatchSaveDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class ProcessPlanDraftManagerFixture {

    final ProcessOrderMapper orderMapper = mock(ProcessOrderMapper.class);
    final OriginalRollMapper rollMapper = mock(OriginalRollMapper.class);
    final ProcessConfigDraftMapper draftMapper = mock(ProcessConfigDraftMapper.class);
    final ProcessPlanDraftPreviewer previewer = mock(ProcessPlanDraftPreviewer.class);
    final DraftOrderVersionGuard versionGuard = mock(DraftOrderVersionGuard.class);
    final ProcessPlanDraftManager manager;

    ProcessPlanDraftManagerFixture() {
        ObjectMapper objectMapper = new ObjectMapper();
        ProcessPlanDraftStore store = new ProcessPlanDraftStore(rollMapper, draftMapper, objectMapper);
        manager = new ProcessPlanDraftManager(
                orderMapper,
                store,
                new ProcessPlanSaveWorkLoader(
                        store, new ProcessPlanBatchTargetFactory(objectMapper)),
                previewer,
                mock(BusinessLockService.class),
                versionGuard,
                new ProcessPlanSavePolicy(objectMapper));
        when(orderMapper.selectById("order-1")).thenReturn(order());
        when(rollMapper.updateById(any(OriginalRoll.class))).thenReturn(1);
        when(draftMapper.insert(any(ProcessConfigDraft.class))).thenReturn(1);
        when(draftMapper.updateById(any(ProcessConfigDraft.class))).thenReturn(1);
        when(previewer.preview(any(), any(), any())).thenAnswer(invocation -> {
            OriginalRoll roll = invocation.getArgument(1);
            return preview(roll.getUuid());
        });
    }

    void useRolls(OriginalRoll... rolls) {
        List<OriginalRoll> values = Arrays.asList(rolls);
        when(rollMapper.selectBatchIds(any())).thenReturn(values);
        for (OriginalRoll roll : values) {
            when(rollMapper.selectById(roll.getUuid())).thenReturn(roll);
        }
    }

    ProcessOrder order() {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(0);
        order.setVersion(7);
        return order;
    }

    OriginalRoll roll(String uuid, int processMode, Integer mainStepType) {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid(uuid);
        roll.setOrderUuid("order-1");
        roll.setProcessMode(processMode);
        roll.setMainStepType(mainStepType);
        roll.setVersion(1);
        return roll;
    }

    ProcessPlanDTO plan(int processMode, Integer mainStepType) {
        ProcessPlanDTO plan = new ProcessPlanDTO();
        plan.setProcessMode(processMode);
        plan.setMainStepType(mainStepType);
        return plan;
    }

    ProcessPlanBatchSaveDTO batch(ProcessPlanDTO plan, String... ids) {
        ProcessPlanBatchSaveDTO dto = new ProcessPlanBatchSaveDTO();
        dto.setExpectedVersion(7);
        dto.setOriginalUuids(Arrays.asList(ids));
        dto.setPlan(plan);
        return dto;
    }

    ProcessPlanItemsBatchSaveDTO items(ProcessPlanDTO plan, String... ids) {
        ProcessPlanItemsBatchSaveDTO dto = new ProcessPlanItemsBatchSaveDTO();
        dto.setExpectedVersion(7);
        dto.setItems(Arrays.stream(ids).map(id -> item(id, plan)).toList());
        return dto;
    }

    ProcessConfigDraft routeDraft(String rollUuid) {
        ProcessConfigDraft draft = new ProcessConfigDraft();
        draft.setUuid("draft-" + rollUuid);
        draft.setOrderUuid("order-1");
        draft.setOriginalUuid(rollUuid);
        draft.setConfigJson("{\"stages\":[]}");
        return draft;
    }

    private ProcessPlanBatchItemDTO item(String id, ProcessPlanDTO plan) {
        ProcessPlanBatchItemDTO item = new ProcessPlanBatchItemDTO();
        item.setOriginalUuid(id);
        item.setPlan(plan);
        return item;
    }

    private PlanPreviewVO preview(String rollUuid) {
        PlanPreviewVO preview = new PlanPreviewVO();
        preview.setOriginalUuid(rollUuid);
        preview.setReady(true);
        return preview;
    }
}

package com.paper.mes.processorder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.dto.ProcessRouteBatchSaveDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessConfigDraftMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessRouteDraftManagerBatchTest {

    private ProcessOrderMapper orderMapper;
    private OriginalRollMapper rollMapper;
    private ProcessConfigDraftMapper draftMapper;
    private ProcessRoutePreviewer previewer;
    private DraftOrderVersionGuard versionGuard;
    private ProcessRouteDraftManager manager;

    @BeforeEach
    void setUp() {
        orderMapper = mock(ProcessOrderMapper.class);
        rollMapper = mock(OriginalRollMapper.class);
        draftMapper = mock(ProcessConfigDraftMapper.class);
        previewer = mock(ProcessRoutePreviewer.class);
        versionGuard = mock(DraftOrderVersionGuard.class);
        manager = new ProcessRouteDraftManager(
                orderMapper,
                rollMapper,
                draftMapper,
                previewer,
                mock(ProcessRoutePriceResolver.class),
                mock(ProcessRoutePersistenceService.class),
                new ObjectMapper(),
                mock(BusinessLockService.class),
                versionGuard);
    }

    @Test
    void saveBatch_twoRoutes_advancesDraftVersionOnce() {
        when(orderMapper.selectById("order-1")).thenReturn(order());
        when(rollMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                roll("roll-1"), roll("roll-2")));
        when(rollMapper.updateById(any(OriginalRoll.class))).thenReturn(1);
        when(previewer.preview(any(OriginalRoll.class), any(ProcessRoutePreviewDTO.class)))
                .thenAnswer(invocation -> preview(invocation.<OriginalRoll>getArgument(0).getUuid()));

        manager.saveBatch("order-1", request());

        verify(versionGuard).assertExpected(any(ProcessOrder.class), org.mockito.ArgumentMatchers.eq(7));
        verify(versionGuard).advance("order-1", 7);
        verify(rollMapper, times(2)).updateById(any(OriginalRoll.class));
    }

    private ProcessRouteBatchSaveDTO request() {
        ProcessRouteBatchSaveDTO dto = new ProcessRouteBatchSaveDTO();
        dto.setExpectedVersion(7);
        dto.setRoutes(List.of(route("roll-1"), route("roll-2")));
        return dto;
    }

    private ProcessRoutePreviewDTO route(String rollUuid) {
        ProcessRoutePreviewDTO.RouteStageDTO stage = new ProcessRoutePreviewDTO.RouteStageDTO();
        stage.setStageLevel(1);
        stage.setStepType(1);
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setOriginalUuid(rollUuid);
        dto.setStages(List.of(stage));
        return dto;
    }

    private ProcessRoutePreviewVO preview(String rollUuid) {
        ProcessRoutePreviewVO.RouteOutputVO output = new ProcessRoutePreviewVO.RouteOutputVO();
        output.setOutputKey("S1-O1");
        output.setConsumedByNextStage(false);
        output.setIsRemain(0);
        ProcessRoutePreviewVO preview = new ProcessRoutePreviewVO();
        preview.setOriginalUuid(rollUuid);
        preview.setOutputs(List.of(output));
        return preview;
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

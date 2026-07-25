package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessRouteSaveServiceVersionTest {

    private ProcessOrderMapper orderMapper;
    private OriginalRollMapper rollMapper;
    private ProcessRoutePreviewer routePreviewer;
    private ProcessRoutePersistenceService persistenceService;
    private ProcessOrderService processOrderService;
    private DraftOrderVersionGuard versionGuard;
    private ProcessRouteSaveService service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(ProcessOrderMapper.class);
        rollMapper = mock(OriginalRollMapper.class);
        routePreviewer = mock(ProcessRoutePreviewer.class);
        persistenceService = mock(ProcessRoutePersistenceService.class);
        processOrderService = mock(ProcessOrderService.class);
        versionGuard = mock(DraftOrderVersionGuard.class);
        service = new ProcessRouteSaveService(
                orderMapper,
                rollMapper,
                routePreviewer,
                persistenceService,
                processOrderService,
                mock(ProcessRoutePriceResolver.class),
                mock(BusinessLockService.class),
                versionGuard,
                mock(ProcessRouteModePolicy.class));
    }

    @Test
    void saveWithStaleVersion_rejectsBeforeReplacingRoute() {
        when(orderMapper.selectById("order-1")).thenReturn(pendingOrder(7));
        when(rollMapper.selectOne(any())).thenReturn(roll());
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.E006))
                .when(versionGuard).assertExpected(any(ProcessOrder.class), eq(6));

        assertThatThrownBy(() -> service.save("order-1", request(6)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.E006.getCode());

        verifyNoInteractions(persistenceService);
    }

    @Test
    void saveWithCurrentVersion_persistsRouteAndLetsFeeCalculationAdvanceVersion() {
        ProcessOrder order = pendingOrder(7);
        OriginalRoll roll = roll();
        ProcessRoutePreviewVO preview = deliverablePreview();
        ProcessRoutePreviewDTO request = request(7);
        when(orderMapper.selectById("order-1")).thenReturn(order);
        when(rollMapper.selectOne(any())).thenReturn(roll);
        when(routePreviewer.preview(roll, request)).thenReturn(preview);

        ProcessRoutePreviewVO result = service.save("order-1", request);

        org.assertj.core.api.Assertions.assertThat(result).isSameAs(preview);
        verify(versionGuard).assertExpected(order, 7);
        verify(persistenceService).replaceRoute(any(ProcessRouteContext.class), eq(request), eq(preview));
        verify(processOrderService).calcFee("order-1");
        verify(versionGuard, org.mockito.Mockito.never()).advance(any(), any());
    }

    private ProcessRoutePreviewDTO request(int expectedVersion) {
        ProcessRoutePreviewDTO.RouteStageDTO stage = new ProcessRoutePreviewDTO.RouteStageDTO();
        stage.setStageLevel(1);
        stage.setStepType(2);
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setExpectedVersion(expectedVersion);
        dto.setOriginalUuid("roll-1");
        dto.setStages(List.of(stage));
        return dto;
    }

    private ProcessOrder pendingOrder(int version) {
        ProcessOrder order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderStatus(1);
        order.setVersion(version);
        return order;
    }

    private OriginalRoll roll() {
        OriginalRoll roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setOrderUuid("order-1");
        return roll;
    }

    private ProcessRoutePreviewVO deliverablePreview() {
        ProcessRoutePreviewVO.RouteOutputVO output = new ProcessRoutePreviewVO.RouteOutputVO();
        output.setConsumedByNextStage(false);
        output.setIsRemain(0);
        ProcessRoutePreviewVO preview = new ProcessRoutePreviewVO();
        preview.setOutputs(List.of(output));
        return preview;
    }
}

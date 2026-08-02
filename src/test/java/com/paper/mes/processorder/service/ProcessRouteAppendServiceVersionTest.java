package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import com.paper.mes.processorder.mapper.ProcessStepMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProcessRouteAppendServiceVersionTest {

    private ProcessOrderMapper orderMapper;
    private OriginalRollMapper rollMapper;
    private ProcessRouteStepWriter stepWriter;
    private ProcessRouteFinishWriter finishWriter;
    private DraftOrderVersionGuard versionGuard;
    private ProcessRouteAppendService service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(ProcessOrderMapper.class);
        rollMapper = mock(OriginalRollMapper.class);
        stepWriter = mock(ProcessRouteStepWriter.class);
        finishWriter = mock(ProcessRouteFinishWriter.class);
        versionGuard = mock(DraftOrderVersionGuard.class);
        service = new ProcessRouteAppendService(
                orderMapper,
                rollMapper,
                mock(ProcessStepMapper.class),
                mock(ProcessRoutePreviewer.class),
                stepWriter,
                finishWriter,
                mock(ProcessRouteExistingOutputResolver.class),
                mock(ProcessRouteSourceConsumer.class),
                mock(ProcessRoutePriceResolver.class),
                mock(ProcessOrderService.class),
                mock(BusinessLockService.class),
                versionGuard);
    }

    @Test
    void saveWithStaleVersion_rejectsBeforeWritingAppendRoute() {
        ProcessOrder order = appendableOrder(8);
        when(orderMapper.selectById("order-1")).thenReturn(order);
        when(rollMapper.selectOne(any())).thenReturn(roll());
        doThrow(new BusinessException(ErrorCode.E006))
                .when(versionGuard).assertExpected(order, 7);

        assertThatThrownBy(() -> service.save("order-1", request(7)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.E006.getCode());

        verifyNoInteractions(stepWriter, finishWriter);
    }

    private ProcessRoutePreviewDTO request(int expectedVersion) {
        ProcessRoutePreviewDTO.RouteStageDTO stage = new ProcessRoutePreviewDTO.RouteStageDTO();
        stage.setStageLevel(2);
        stage.setStepType(2);
        ProcessRoutePreviewDTO dto = new ProcessRoutePreviewDTO();
        dto.setExpectedVersion(expectedVersion);
        dto.setOriginalUuid("roll-1");
        dto.setStages(List.of(stage));
        return dto;
    }

    private ProcessOrder appendableOrder(int version) {
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
}

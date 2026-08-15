package com.paper.mes.processorder.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.inventory.service.InventoryLedgerBusinessRecorder;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.dto.ProcessRollDispositionDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.entity.ProcessRollDisposition;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessRollDispositionMapper;
import com.paper.mes.processorder.model.ProcessRollDispositionAction;
import com.paper.mes.processorder.service.BackRecordDirectShipRecorder;
import com.paper.mes.processorder.service.BackRecordWarehousePolicy;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.processorder.service.ProcessRouteCleanupService;
import com.paper.mes.settle.mapper.SettleDetailMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessRollDispositionServiceImplTest {

    @Mock private OriginalRollMapper rollMapper;
    @Mock private FinishRollMapper finishMapper;
    @Mock private FinishOriginalRelMapper relationMapper;
    @Mock private ProcessRollDispositionMapper dispositionMapper;
    @Mock private DeliveryDetailMapper deliveryDetailMapper;
    @Mock private SettleDetailMapper settleDetailMapper;
    @Mock private ProcessOrderService orderService;
    @Mock private ProcessRouteCleanupService routeCleanupService;
    @Mock private BackRecordDirectShipRecorder directShipRecorder;
    @Mock private BackRecordWarehousePolicy backRecordWarehousePolicy;
    @Mock private InventoryLedgerBusinessRecorder inventoryRecorder;
    @Mock private OperationLogService operationLogService;
    @Mock private BusinessLockService businessLockService;
    @InjectMocks private ProcessRollDispositionServiceImpl service;

    private ProcessOrder order;
    private OriginalRoll roll;

    @BeforeEach
    void setUp() {
        order = new ProcessOrder();
        order.setUuid("order-1");
        order.setOrderNo("JG-001");
        order.setOrderStatus(2);
        order.setVersion(7);
        roll = new OriginalRoll();
        roll.setUuid("roll-1");
        roll.setOrderUuid(order.getUuid());
        roll.setRollNo("R-001");
        roll.setIsChecked(0);
        roll.setRollStatus(2);
    }

    private void stubHappyPath() {
        when(dispositionMapper.selectOne(any())).thenReturn(null);
        when(rollMapper.selectById(roll.getUuid())).thenReturn(roll);
        when(orderService.getById(order.getUuid())).thenReturn(order);
        when(settleDetailMapper.selectCount(any())).thenReturn(0L);
        when(relationMapper.selectList(any())).thenReturn(List.of());
        when(orderService.updateById(order)).thenReturn(true);
        when(rollMapper.updateById(roll)).thenReturn(1);
        when(dispositionMapper.insert(any(ProcessRollDisposition.class))).thenReturn(1);
    }

    @Test
    void cancel_recordsDisposition_withoutMarkingPhysicalScrap() {
        stubHappyPath();
        ProcessRollDispositionDTO command = command(ProcessRollDispositionAction.CANCEL, 7);

        var result = service.dispose(roll.getUuid(), command);

        assertThat(result.getAction()).isEqualTo(ProcessRollDispositionAction.CANCEL);
        assertThat(roll.getRollStatus()).isEqualTo(2);
        assertThat(roll.getDispositionAction()).isEqualTo(ProcessRollDispositionAction.CANCEL);
        assertThat(roll.getIsChecked()).isEqualTo(1);
        verify(routeCleanupService).clearExistingRoute(any());
        verify(orderService).calcFee(order.getUuid());
        verify(dispositionMapper).insert(any(ProcessRollDisposition.class));
    }

    @Test
    void staleOrderVersion_isRejectedBeforeRouteMutation() {
        when(dispositionMapper.selectOne(any())).thenReturn(null);
        when(rollMapper.selectById(roll.getUuid())).thenReturn(roll);
        when(orderService.getById(order.getUuid())).thenReturn(order);
        ProcessRollDispositionDTO command = command(ProcessRollDispositionAction.CANCEL, 6);

        assertThatThrownBy(() -> service.dispose(roll.getUuid(), command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("发生变化");

        verify(routeCleanupService, never()).clearExistingRoute(any());
        verify(dispositionMapper, never()).insert(any(ProcessRollDisposition.class));
    }

    @Test
    void repeatedRequest_returnsOriginalResult_withoutApplyingAgain() {
        ProcessRollDisposition applied = new ProcessRollDisposition();
        applied.setSourceOrderUuid(order.getUuid());
        applied.setSourceRollUuid(roll.getUuid());
        applied.setActionType(ProcessRollDispositionAction.CANCEL);
        applied.setRequestId("request-1");
        when(dispositionMapper.selectOne(any())).thenReturn(applied);
        when(rollMapper.selectById(roll.getUuid())).thenReturn(roll);
        when(orderService.getById(order.getUuid())).thenReturn(order);

        var result = service.dispose(roll.getUuid(), command(ProcessRollDispositionAction.CANCEL, 7));

        assertThat(result.getAction()).isEqualTo(ProcessRollDispositionAction.CANCEL);
        verify(routeCleanupService, never()).clearExistingRoute(any());
        verify(dispositionMapper, never()).insert(any(ProcessRollDisposition.class));
    }

    @Test
    void checkedRoll_isRejected_withoutRouteMutation() {
        roll.setIsChecked(1);
        when(dispositionMapper.selectOne(any())).thenReturn(null);
        when(rollMapper.selectById(roll.getUuid())).thenReturn(roll);
        when(orderService.getById(order.getUuid())).thenReturn(order);

        assertThatThrownBy(() -> service.dispose(roll.getUuid(), command(ProcessRollDispositionAction.CANCEL, 7)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("回录");

        verify(routeCleanupService, never()).clearExistingRoute(any());
    }

    @Test
    void splitToOrder_createsEditableTarget_andRecalculatesSourceFee() {
        stubHappyPath();
        OriginalRoll target = new OriginalRoll();
        target.setUuid("target-roll-1");
        ProcessOrder targetOrder = new ProcessOrder();
        targetOrder.setUuid("target-order-1");
        targetOrder.setOrderNo("JG-002");
        when(orderService.create(any())).thenReturn("target-order-1");
        when(orderService.getById("target-order-1")).thenReturn(targetOrder);
        when(rollMapper.selectOne(any())).thenReturn(target);

        var result = service.dispose(roll.getUuid(), command(ProcessRollDispositionAction.SPLIT_TO_ORDER, 7));

        assertThat(result.getTargetOrderUuid()).isEqualTo("target-order-1");
        assertThat(result.getTargetOrderNo()).isEqualTo("JG-002");
        assertThat(result.getTargetRollUuid()).isEqualTo("target-roll-1");
        verify(orderService).calcFee(order.getUuid());
        assertThat(roll.getRollStatus()).isEqualTo(2);
        assertThat(roll.getDispositionAction()).isEqualTo(ProcessRollDispositionAction.SPLIT_TO_ORDER);
    }

    @Test
    void directShip_marksSourceAsDirect_andCreatesInventoryReceipt() {
        stubHappyPath();
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setActualWeight(new java.math.BigDecimal("2000"));
        when(finishMapper.updateById(finish)).thenReturn(1);
        when(directShipRecorder.record(any(), any(), any()))
                .thenReturn(new BackRecordDirectShipRecorder.Result(1, List.of(finish)));

        ProcessRollDispositionDTO dto = command(ProcessRollDispositionAction.DIRECT_SHIP, 7);
        dto.setWarehouseUuid("warehouse-1");
        dto.setActualWeight(new java.math.BigDecimal("2000"));
        when(backRecordWarehousePolicy.requireEnabled("warehouse-1"))
                .thenReturn(new BackRecordWarehousePolicy.WarehouseSnapshot("warehouse-1", "测试仓", null));

        service.dispose(roll.getUuid(), dto);

        assertThat(order.getWarehouseUuid()).isEqualTo("warehouse-1");
        assertThat(roll.getProcessMode()).isEqualTo(3);
        assertThat(roll.getMainStepType()).isNull();
        assertThat(roll.getRollStatus()).isEqualTo(4);
        assertThat(roll.getDispositionAction()).isEqualTo(ProcessRollDispositionAction.DIRECT_SHIP);
        assertThat(roll.getWeightStatus()).isEqualTo("MEASURED");
        assertThat(roll.getWeightSource()).isEqualTo("SCALE");
        assertThat(roll.getWeightRecordedAt()).isNotNull();
        assertThat(roll.getWeightRecordedBy()).isNotBlank();
        verify(orderService, org.mockito.Mockito.times(2)).updateById(order);
        verify(inventoryRecorder).receipt(eq(finish), eq(order.getUuid()),
                eq("DISPOSITION:" + dto.getRequestId()), any());
        verify(orderService).calcFee(order.getUuid());
    }

    @Test
    void directShip_onlyBuildsAndReceiptsTheDisposedRoll() {
        stubHappyPath();
        order.setWarehouseUuid("warehouse-1");
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setActualWeight(new java.math.BigDecimal("2000"));
        when(finishMapper.updateById(finish)).thenReturn(1);
        when(directShipRecorder.record(any(), any(), any()))
                .thenReturn(new BackRecordDirectShipRecorder.Result(1, List.of(finish)));

        ProcessRollDispositionDTO dto = command(ProcessRollDispositionAction.DIRECT_SHIP, 7);
        dto.setWarehouseUuid("warehouse-1");
        dto.setActualWeight(new java.math.BigDecimal("2000"));
        when(backRecordWarehousePolicy.requireEnabled("warehouse-1"))
                .thenReturn(new BackRecordWarehousePolicy.WarehouseSnapshot("warehouse-1", "测试仓", null));

        service.dispose(roll.getUuid(), dto);

        ArgumentCaptor<List<OriginalRoll>> allSources = ArgumentCaptor.forClass(List.class);
        verify(directShipRecorder).record(any(), any(), allSources.capture());
        assertThat(allSources.getValue()).extracting(OriginalRoll::getUuid)
                .containsExactly(roll.getUuid());
    }

    @Test
    void directShip_persistsEveryGeneratedFinishUuid_forMultiPieceSource() {
        stubHappyPath();
        FinishRoll first = new FinishRoll();
        first.setUuid("finish-1");
        FinishRoll second = new FinishRoll();
        second.setUuid("finish-2");
        when(finishMapper.updateById(any(FinishRoll.class))).thenReturn(1);
        when(directShipRecorder.record(any(), any(), any()))
                .thenReturn(new BackRecordDirectShipRecorder.Result(2, List.of(first, second)));

        ProcessRollDispositionDTO dto = command(ProcessRollDispositionAction.DIRECT_SHIP, 7);
        dto.setWarehouseUuid("warehouse-1");
        dto.setActualWeight(new java.math.BigDecimal("2000"));
        when(backRecordWarehousePolicy.requireEnabled("warehouse-1"))
                .thenReturn(new BackRecordWarehousePolicy.WarehouseSnapshot("warehouse-1", "测试仓", null));

        var result = service.dispose(roll.getUuid(), dto);

        assertThat(result.getTargetFinishUuids()).containsExactly("finish-1", "finish-2");
        ArgumentCaptor<ProcessRollDisposition> audit = ArgumentCaptor.forClass(ProcessRollDisposition.class);
        verify(dispositionMapper).insert(audit.capture());
        assertThat(audit.getValue().getTargetFinishUuid()).isEqualTo("finish-1");
        assertThat(audit.getValue().getTargetFinishUuids()).isEqualTo("[\"finish-1\",\"finish-2\"]");
    }

    private ProcessRollDispositionDTO command(ProcessRollDispositionAction action, int version) {
        ProcessRollDispositionDTO dto = new ProcessRollDispositionDTO();
        dto.setAction(action);
        dto.setRequestId("request-1");
        dto.setReason("客户取消本次加工");
        dto.setExpectedOrderVersion(version);
        return dto;
    }
}

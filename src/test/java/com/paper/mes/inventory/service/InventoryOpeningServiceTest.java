package com.paper.mes.inventory.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.inventory.dto.InventoryLedgerCommand;
import com.paper.mes.inventory.dto.InventoryOpeningRequest;
import com.paper.mes.inventory.entity.InventoryLedgerEntry;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.oplog.service.OperationLogService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryOpeningServiceTest {

    @BeforeAll
    static void initTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, FinishRoll.class);
        TableInfoHelper.initTableInfo(assistant, DeliveryDetail.class);
    }

    @Test
    void openingUsesCurrentProjectionAndWritesOpeningBalanceOncePerFinishRoll() {
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        DeliveryDetailMapper detailMapper = mock(DeliveryDetailMapper.class);
        InventoryLedgerService ledger = mock(InventoryLedgerService.class);
        OperationLogService operationLogs = mock(OperationLogService.class);
        FinishRoll finish = finish("finish-1", "100.000");
        when(finishMapper.selectList(any())).thenReturn(List.of(finish));
        when(detailMapper.selectList(any())).thenReturn(List.of());
        when(ledger.openBalance(any())).thenAnswer(invocation -> openingEntry(invocation.getArgument(0)));
        BusinessLockService locks = mock(BusinessLockService.class);
        InventoryOpeningService service = new InventoryOpeningService(
                finishMapper, new InventoryOpeningReservationReader(detailMapper), locks, ledger, operationLogs);
        InventoryOpeningRequest request = new InventoryOpeningRequest();
        request.setSwitchUuid("switch-20260802");
        request.setOccurredAt(LocalDateTime.of(2026, 8, 2, 10, 0));

        var reconciliation = service.openCurrentProjection(request);

        assertThat(reconciliation.lines()).hasSize(1);
        assertThat(reconciliation.lines().getFirst().projectedWeight()).isEqualByComparingTo("100.000");
        assertThat(reconciliation.lines().getFirst().weightDifference()).isZero();
        var lockOrder = inOrder(locks);
        lockOrder.verify(locks).lockFinishRolls(List.of("finish-1"));
        lockOrder.verify(locks).lockInventorySwitch();
        verify(operationLogs).record(eq(OperationLogService.BIZ_TYPE_INVENTORY), eq("switch-20260802"),
                eq("switch-20260802"), eq(OperationLogService.ACTION_INVENTORY_OPENING), eq(null),
                startsWith("lines=1;"));
    }

    @Test
    void openingAddsActiveReservationToPhysicalBalanceButReconcilesAvailableWeight() {
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        DeliveryDetailMapper detailMapper = mock(DeliveryDetailMapper.class);
        InventoryLedgerService ledger = mock(InventoryLedgerService.class);
        OperationLogService operationLogs = mock(OperationLogService.class);
        FinishRoll finish = finish("finish-1", "90.000");
        DeliveryDetail reservation = new DeliveryDetail();
        reservation.setFinishUuid("finish-1");
        reservation.setStockLockStatus(1);
        reservation.setOutWeight(new BigDecimal("10.000"));
        when(finishMapper.selectList(any())).thenReturn(List.of(finish));
        when(detailMapper.selectList(any())).thenReturn(List.of(reservation));
        when(ledger.openBalance(any())).thenAnswer(invocation -> openingEntry(invocation.getArgument(0)));
        InventoryOpeningService service = new InventoryOpeningService(
                finishMapper, new InventoryOpeningReservationReader(detailMapper),
                mock(BusinessLockService.class), ledger, operationLogs);
        InventoryOpeningRequest request = new InventoryOpeningRequest();
        request.setSwitchUuid("switch-20260802");
        request.setOccurredAt(LocalDateTime.of(2026, 8, 2, 10, 0));

        var reconciliation = service.openCurrentProjection(request);

        var command = forClass(InventoryLedgerCommand.class);
        verify(ledger).openBalance(command.capture());
        assertThat(command.getValue().getWeightDelta()).isEqualByComparingTo("100.000");
        assertThat(command.getValue().getReservedWeightDelta()).isEqualByComparingTo("10.000");
        assertThat(reconciliation.lines().getFirst().openingWeight()).isEqualByComparingTo("90.000");
        assertThat(reconciliation.lines().getFirst().weightDifference()).isZero();
    }

    @Test
    void openingMismatch_isRejectedInsteadOfCommittingAnUnreconciledLedger() {
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        DeliveryDetailMapper detailMapper = mock(DeliveryDetailMapper.class);
        InventoryLedgerService ledger = mock(InventoryLedgerService.class);
        OperationLogService operationLogs = mock(OperationLogService.class);
        when(finishMapper.selectList(any())).thenReturn(List.of(finish("finish-1", "100.000")));
        when(detailMapper.selectList(any())).thenReturn(List.of());
        when(ledger.openBalance(any())).thenAnswer(invocation -> {
            InventoryLedgerEntry entry = openingEntry(invocation.getArgument(0));
            entry.setAvailableWeightAfter(new BigDecimal("99.000"));
            return entry;
        });
        InventoryOpeningService service = new InventoryOpeningService(
                finishMapper, new InventoryOpeningReservationReader(detailMapper),
                mock(BusinessLockService.class), ledger, operationLogs);
        InventoryOpeningRequest request = new InventoryOpeningRequest();
        request.setSwitchUuid("switch-mismatch");
        request.setOccurredAt(LocalDateTime.of(2026, 8, 2, 10, 0));

        assertThatThrownBy(() -> service.openCurrentProjection(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reconciliation mismatch");
        org.mockito.Mockito.verifyNoInteractions(operationLogs);
    }

    @Test
    void previewReturnsProjectionWithoutWritingLedger() {
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        DeliveryDetailMapper detailMapper = mock(DeliveryDetailMapper.class);
        InventoryLedgerService ledger = mock(InventoryLedgerService.class);
        FinishRoll finish = finish("finish-preview", "42.000");
        when(finishMapper.selectList(any())).thenReturn(List.of(finish));
        when(detailMapper.selectList(any())).thenReturn(List.of());
        InventoryOpeningService service = new InventoryOpeningService(
                finishMapper, new InventoryOpeningReservationReader(detailMapper),
                mock(BusinessLockService.class), ledger,
                mock(OperationLogService.class));
        InventoryOpeningRequest request = new InventoryOpeningRequest();
        request.setSwitchUuid("switch-preview");
        request.setOccurredAt(LocalDateTime.of(2026, 8, 2, 10, 0));

        var reconciliation = service.previewCurrentProjection(request);

        assertThat(reconciliation.matched()).isTrue();
        assertThat(reconciliation.preview()).isTrue();
        assertThat(reconciliation.lines().getFirst().projectedWeight()).isEqualByComparingTo("42.000");
        verify(ledger, org.mockito.Mockito.never()).openBalance(any());
    }

    @Test
    void openingRejectsActiveReservationForMissingFinishRoll() {
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        DeliveryDetailMapper detailMapper = mock(DeliveryDetailMapper.class);
        FinishRoll finish = finish("finish-1", "10.000");
        DeliveryDetail reservation = new DeliveryDetail();
        reservation.setFinishUuid("missing-finish");
        reservation.setStockLockStatus(1);
        reservation.setOutWeight(new BigDecimal("2.000"));
        when(finishMapper.selectList(any())).thenReturn(List.of(finish));
        when(detailMapper.selectList(any())).thenReturn(List.of(reservation));
        InventoryOpeningService service = new InventoryOpeningService(
                finishMapper, new InventoryOpeningReservationReader(detailMapper),
                mock(BusinessLockService.class),
                mock(InventoryLedgerService.class), mock(OperationLogService.class));

        InventoryOpeningRequest request = new InventoryOpeningRequest();
        request.setSwitchUuid("switch-orphan");
        request.setOccurredAt(LocalDateTime.of(2026, 8, 2, 10, 0));

        assertThatThrownBy(() -> service.openCurrentProjection(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("活跃出库占用关联成品卷不存在");
    }

    @Test
    void openingRejectsActiveReservationForNonStockFinishRoll() {
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        DeliveryDetailMapper detailMapper = mock(DeliveryDetailMapper.class);
        FinishRoll finish = finish("finish-shipped", "10.000");
        finish.setFinishStatus(3);
        DeliveryDetail reservation = new DeliveryDetail();
        reservation.setFinishUuid("finish-shipped");
        reservation.setStockLockStatus(1);
        reservation.setOutWeight(new BigDecimal("2.000"));
        when(finishMapper.selectList(any())).thenReturn(List.of(finish));
        when(detailMapper.selectList(any())).thenReturn(List.of(reservation));
        InventoryOpeningService service = new InventoryOpeningService(
                finishMapper, new InventoryOpeningReservationReader(detailMapper),
                mock(BusinessLockService.class), mock(InventoryLedgerService.class),
                mock(OperationLogService.class));

        InventoryOpeningRequest request = new InventoryOpeningRequest();
        request.setSwitchUuid("switch-non-stock");
        request.setOccurredAt(LocalDateTime.of(2026, 8, 2, 10, 0));

        assertThatThrownBy(() -> service.openCurrentProjection(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非在库成品卷存在活跃出库占用");
    }

    private FinishRoll finish(String uuid, String remainingWeight) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setFinishRollNo("F000001");
        finish.setFinishStatus(2);
        finish.setActualWeight(new BigDecimal(remainingWeight));
        finish.setRemainingWeight(new BigDecimal(remainingWeight));
        return finish;
    }

    private InventoryLedgerEntry openingEntry(InventoryLedgerCommand command) {
        InventoryLedgerEntry entry = new InventoryLedgerEntry();
        entry.setAvailableQuantityAfter(command.getQuantityDelta());
        entry.setAvailableWeightAfter(command.getWeightDelta().subtract(command.getReservedWeightDelta()));
        return entry;
    }
}

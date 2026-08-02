package com.paper.mes.inventory.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.inventory.dto.InventoryLedgerCommand;
import com.paper.mes.inventory.dto.InventoryOpeningRequest;
import com.paper.mes.inventory.entity.InventoryLedgerEntry;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        FinishRoll finish = finish("finish-1", "100.000");
        when(finishMapper.selectList(any())).thenReturn(List.of(finish));
        when(detailMapper.selectList(any())).thenReturn(List.of());
        when(ledger.openBalance(any())).thenAnswer(invocation -> openingEntry(invocation.getArgument(0)));
        InventoryOpeningService service = new InventoryOpeningService(
                finishMapper, detailMapper, mock(BusinessLockService.class), ledger);
        InventoryOpeningRequest request = new InventoryOpeningRequest();
        request.setSwitchUuid("switch-20260802");
        request.setOccurredAt(LocalDateTime.of(2026, 8, 2, 10, 0));

        var reconciliation = service.openCurrentProjection(request);

        assertThat(reconciliation.lines()).hasSize(1);
        assertThat(reconciliation.lines().getFirst().projectedWeight()).isEqualByComparingTo("100.000");
        assertThat(reconciliation.lines().getFirst().weightDifference()).isZero();
    }

    @Test
    void openingAddsActiveReservationToPhysicalBalanceButReconcilesAvailableWeight() {
        FinishRollMapper finishMapper = mock(FinishRollMapper.class);
        DeliveryDetailMapper detailMapper = mock(DeliveryDetailMapper.class);
        InventoryLedgerService ledger = mock(InventoryLedgerService.class);
        FinishRoll finish = finish("finish-1", "90.000");
        DeliveryDetail reservation = new DeliveryDetail();
        reservation.setFinishUuid("finish-1");
        reservation.setStockLockStatus(1);
        reservation.setOutWeight(new BigDecimal("10.000"));
        when(finishMapper.selectList(any())).thenReturn(List.of(finish));
        when(detailMapper.selectList(any())).thenReturn(List.of(reservation));
        when(ledger.openBalance(any())).thenAnswer(invocation -> openingEntry(invocation.getArgument(0)));
        InventoryOpeningService service = new InventoryOpeningService(
                finishMapper, detailMapper, mock(BusinessLockService.class), ledger);
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

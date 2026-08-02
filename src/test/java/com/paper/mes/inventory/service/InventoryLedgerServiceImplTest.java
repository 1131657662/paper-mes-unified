package com.paper.mes.inventory.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.inventory.dto.InventoryLedgerCommand;
import com.paper.mes.inventory.entity.InventoryLedgerEntry;
import com.paper.mes.inventory.entity.InventoryLedgerEventType;
import com.paper.mes.inventory.mapper.InventoryLedgerMapper;
import com.paper.mes.inventory.service.impl.InventoryLedgerServiceImpl;
import com.paper.mes.processorder.entity.FinishRoll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryLedgerServiceImplTest {

    @Test
    void openBalance_buildsFirstPhysicalAndAvailableBalance() {
        InventoryLedgerMapper mapper = mock(InventoryLedgerMapper.class);
        BusinessLockService locks = mock(BusinessLockService.class);
        when(mapper.selectByIdempotencyKey(any())).thenReturn(null);
        when(mapper.selectLatestForUpdate("finish-1")).thenReturn(null);
        when(mapper.insert(any())).thenReturn(1);

        InventoryLedgerEntry result = service(mapper, locks).openBalance(command(
                InventoryLedgerEventType.OPENING_BALANCE, "OPENING_BALANCE", "switch-1",
                new BigDecimal("1"), new BigDecimal("100"), "opening-1"));

        assertThat(result.getQuantityBefore()).isZero();
        assertThat(result.getQuantityAfter()).isEqualByComparingTo("1");
        assertThat(result.getWeightAfter()).isEqualByComparingTo("100");
        assertThat(result.getAvailableWeightAfter()).isEqualByComparingTo("100");
        verify(locks).lockFinishRolls(List.of("finish-1"));
    }

    @Test
    void openBalance_allowsZeroProjectionToAnchorSwitchDay() {
        InventoryLedgerMapper mapper = mock(InventoryLedgerMapper.class);
        when(mapper.selectByIdempotencyKey(any())).thenReturn(null);
        when(mapper.selectLatestForUpdate("finish-1")).thenReturn(null);
        when(mapper.insert(any())).thenReturn(1);

        InventoryLedgerCommand command = command(InventoryLedgerEventType.OPENING_BALANCE,
                "INVENTORY_SWITCH", "switch-1", BigDecimal.ZERO, BigDecimal.ZERO, "opening-zero-1");
        command.setOperatorName(null);
        InventoryLedgerEntry result = service(mapper, mock(BusinessLockService.class)).openBalance(command);

        assertThat(result.getWeightAfter()).isZero();
        assertThat(result.getOperatorName()).isEqualTo("system");
    }

    @Test
    void reserve_changesReservedAndAvailableBalancesWithoutChangingPhysicalStock() {
        InventoryLedgerMapper mapper = mock(InventoryLedgerMapper.class);
        BusinessLockService locks = mock(BusinessLockService.class);
        when(mapper.selectByIdempotencyKey(any())).thenReturn(null);
        when(mapper.selectLatestForUpdate("finish-1")).thenReturn(previous("opening-1"));
        when(mapper.insert(any())).thenReturn(1);

        InventoryLedgerCommand command = command(InventoryLedgerEventType.RESERVE,
                "DELIVERY_ORDER", "delivery-1", BigDecimal.ZERO, BigDecimal.ZERO, "reserve-1");
        command.setReservedWeightDelta(new BigDecimal("10"));
        InventoryLedgerEntry result = service(mapper, locks).append(command);

        assertThat(result.getWeightAfter()).isEqualByComparingTo("100");
        assertThat(result.getReservedWeightAfter()).isEqualByComparingTo("10");
        assertThat(result.getAvailableWeightAfter()).isEqualByComparingTo("90");
    }

    @Test
    void issueThatExceedsPhysicalBalanceIsRejectedBeforeInsert() {
        InventoryLedgerMapper mapper = mock(InventoryLedgerMapper.class);
        BusinessLockService locks = mock(BusinessLockService.class);
        when(mapper.selectByIdempotencyKey(any())).thenReturn(null);
        when(mapper.selectLatestForUpdate("finish-1")).thenReturn(previous("opening-1"));

        InventoryLedgerCommand command = command(InventoryLedgerEventType.ISSUE,
                "DELIVERY_ORDER", "delivery-1", BigDecimal.ZERO, new BigDecimal("-101"), "issue-1");

        assertThatThrownBy(() -> service(mapper, locks).append(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void openingBalanceCannotBeAppendedThroughGenericCommand() {
        InventoryLedgerMapper mapper = mock(InventoryLedgerMapper.class);
        InventoryLedgerCommand command = command(InventoryLedgerEventType.OPENING_BALANCE,
                "INVENTORY_SWITCH", "switch-1", BigDecimal.ONE, BigDecimal.ONE, "opening-1");

        assertThatThrownBy(() -> service(mapper, mock(BusinessLockService.class)).append(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("explicit opening");
    }

    @Test
    void idempotencyKeyReuseWithDifferentPayloadIsRejected() {
        InventoryLedgerMapper mapper = mock(InventoryLedgerMapper.class);
        InventoryLedgerEntry existing = new InventoryLedgerEntry();
        existing.setPayloadHash("different-payload");
        when(mapper.selectByIdempotencyKey("reserve-1")).thenReturn(existing);
        InventoryLedgerCommand reserve = command(InventoryLedgerEventType.RESERVE,
                "DELIVERY_ORDER", "delivery-1", BigDecimal.ZERO, BigDecimal.ZERO, "reserve-1");
        reserve.setReservedWeightDelta(new BigDecimal("10"));

        assertThatThrownBy(() -> service(mapper, mock(BusinessLockService.class)).append(reserve))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("idempotency");
    }

    @Test
    void reserveAsFirstEventIsRejectedUntilOpeningOrReceiptExists() {
        InventoryLedgerMapper mapper = mock(InventoryLedgerMapper.class);
        when(mapper.selectByIdempotencyKey(any())).thenReturn(null);
        when(mapper.selectLatestForUpdate("finish-1")).thenReturn(null);
        InventoryLedgerCommand reserve = command(InventoryLedgerEventType.RESERVE,
                "DELIVERY_ORDER", "delivery-1", BigDecimal.ZERO, BigDecimal.ZERO, "reserve-1");
        reserve.setReservedWeightDelta(new BigDecimal("10"));

        assertThatThrownBy(() -> service(mapper, mock(BusinessLockService.class)).append(reserve))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no opening or receipt");
    }

    @Test
    void confirmRollbackConfirmCycleBuildsASecondIssueBalance() {
        InventoryLedgerMapper mapper = mock(InventoryLedgerMapper.class);
        BusinessLockService locks = mock(BusinessLockService.class);
        List<InventoryLedgerEntry> entries = new ArrayList<>();
        Map<String, InventoryLedgerEntry> byKey = new HashMap<>();
        when(mapper.selectByIdempotencyKey(any())).thenAnswer(invocation ->
                byKey.get(invocation.getArgument(0, String.class)));
        when(mapper.selectLatestForUpdate("finish-1")).thenAnswer(invocation ->
                entries.isEmpty() ? null : entries.get(entries.size() - 1));
        when(mapper.insert(any())).thenAnswer(invocation -> {
            InventoryLedgerEntry entry = invocation.getArgument(0);
            entry.setSequenceNo((long) entries.size() + 1);
            entries.add(entry);
            byKey.put(entry.getIdempotencyKey(), entry);
            return 1;
        });

        InventoryLedgerServiceImpl ledger = service(mapper, locks);
        InventoryLedgerBusinessRecorder recorder = new InventoryLedgerBusinessRecorder(ledger);
        FinishRoll finish = new FinishRoll();
        finish.setUuid("finish-1");
        finish.setActualWeight(new BigDecimal("100.000"));

        ledger.openBalance(command(InventoryLedgerEventType.OPENING_BALANCE,
                "INVENTORY_SWITCH", "switch-1", BigDecimal.ONE,
                new BigDecimal("100.000"), "opening-1"));
        recorder.reserve(finish, "delivery-1", "detail-1", new BigDecimal("40.000"), null);
        recorder.issue(finish, "delivery-1", "detail-1", new BigDecimal("40.000"), false, 1, null);
        recorder.returned(finish, "delivery-1", "detail-1", new BigDecimal("40.000"), false, 2, null);
        recorder.issue(finish, "delivery-1", "detail-1", new BigDecimal("40.000"), false, 3, null);

        assertThat(entries).hasSize(5);
        assertThat(entries.get(2).getIdempotencyKey()).isEqualTo("ISSUE:detail-1:1");
        assertThat(entries.get(4).getIdempotencyKey()).isEqualTo("ISSUE:detail-1:3");
        assertThat(entries.get(4).getWeightAfter()).isEqualByComparingTo("60.000");
        assertThat(entries.get(4).getReservedWeightAfter()).isZero();
        verify(mapper, times(5)).insert(any());
    }

    private InventoryLedgerServiceImpl service(InventoryLedgerMapper mapper, BusinessLockService locks) {
        return new InventoryLedgerServiceImpl(mapper, locks);
    }

    private InventoryLedgerCommand command(InventoryLedgerEventType eventType, String sourceType,
                                           String sourceUuid, BigDecimal quantity, BigDecimal weight,
                                           String idempotencyKey) {
        InventoryLedgerCommand command = new InventoryLedgerCommand();
        command.setFinishRollUuid("finish-1");
        command.setEventType(eventType);
        command.setSourceBusinessType(sourceType);
        command.setSourceBusinessUuid(sourceUuid);
        command.setQuantityDelta(quantity);
        command.setWeightDelta(weight);
        command.setReservedQuantityDelta(BigDecimal.ZERO);
        command.setReservedWeightDelta(BigDecimal.ZERO);
        command.setOperatorName("operator");
        command.setOccurredAt(LocalDateTime.of(2026, 8, 2, 10, 0));
        command.setIdempotencyKey(idempotencyKey);
        return command;
    }

    private InventoryLedgerEntry previous(String uuid) {
        InventoryLedgerEntry previous = new InventoryLedgerEntry();
        previous.setUuid(uuid);
        previous.setQuantityAfter(new BigDecimal("1"));
        previous.setWeightAfter(new BigDecimal("100"));
        previous.setReservedQuantityAfter(BigDecimal.ZERO);
        previous.setReservedWeightAfter(BigDecimal.ZERO);
        return previous;
    }
}

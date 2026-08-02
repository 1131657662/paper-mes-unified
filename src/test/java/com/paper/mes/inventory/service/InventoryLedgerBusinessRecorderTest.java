package com.paper.mes.inventory.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.inventory.dto.InventoryLedgerCommand;
import com.paper.mes.inventory.entity.InventoryLedgerEntry;
import com.paper.mes.inventory.entity.InventoryLedgerEventType;
import com.paper.mes.processorder.entity.FinishRoll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryLedgerBusinessRecorderTest {

    @Test
    void issueSynchronizesPhysicalAndReservedWeight() {
        InventoryLedgerService ledger = mock(InventoryLedgerService.class);
        when(ledger.append(any())).thenReturn(new InventoryLedgerEntry());
        InventoryLedgerBusinessRecorder recorder = new InventoryLedgerBusinessRecorder(ledger);
        FinishRoll finish = finish("finish-1", "100.000");

        recorder.issue(finish, "delivery-1", "detail-1", new BigDecimal("40.000"), false, 1, null);

        ArgumentCaptor<InventoryLedgerCommand> captor = ArgumentCaptor.forClass(InventoryLedgerCommand.class);
        verify(ledger).append(captor.capture());
        InventoryLedgerCommand command = captor.getValue();
        assertThat(command.getWeightDelta()).isEqualByComparingTo("-40.000");
        assertThat(command.getReservedWeightDelta()).isEqualByComparingTo("-40.000");
        assertThat(command.getQuantityDelta()).isZero();
        assertThat(command.getIdempotencyKey()).isEqualTo("ISSUE:detail-1:1");
    }

    @Test
    void issueUsesANewKeyWhenTheSameDetailIsConfirmedAgain() {
        InventoryLedgerService ledger = mock(InventoryLedgerService.class);
        when(ledger.append(any())).thenReturn(new InventoryLedgerEntry());
        InventoryLedgerBusinessRecorder recorder = new InventoryLedgerBusinessRecorder(ledger);
        FinishRoll finish = finish("finish-1", "100.000");

        recorder.issue(finish, "delivery-1", "detail-1", new BigDecimal("40.000"), false, 1, null);
        recorder.issue(finish, "delivery-1", "detail-1", new BigDecimal("40.000"), false, 3, null);

        ArgumentCaptor<InventoryLedgerCommand> captor = ArgumentCaptor.forClass(InventoryLedgerCommand.class);
        verify(ledger, times(2)).append(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(InventoryLedgerCommand::getIdempotencyKey)
                .containsExactly("ISSUE:detail-1:1", "ISSUE:detail-1:3");
    }

    @Test
    void returnOfWholeRollRestoresPhysicalAndReservedQuantity() {
        InventoryLedgerService ledger = mock(InventoryLedgerService.class);
        when(ledger.append(any())).thenReturn(new InventoryLedgerEntry());
        InventoryLedgerBusinessRecorder recorder = new InventoryLedgerBusinessRecorder(ledger);

        recorder.returned(finish("finish-1", "100.000"), "delivery-1", "detail-1",
                new BigDecimal("100.000"), true, 2, null);

        ArgumentCaptor<InventoryLedgerCommand> captor = ArgumentCaptor.forClass(InventoryLedgerCommand.class);
        verify(ledger).append(captor.capture());
        assertThat(captor.getValue().getQuantityDelta()).isEqualByComparingTo("1");
        assertThat(captor.getValue().getReservedWeightDelta()).isEqualByComparingTo("100");
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("RETURN:detail-1:2");
    }

    @Test
    void scrapCarriesReasonAndStableIdempotencyKey() {
        InventoryLedgerService ledger = mock(InventoryLedgerService.class);
        when(ledger.append(any())).thenReturn(new InventoryLedgerEntry());
        InventoryLedgerBusinessRecorder recorder = new InventoryLedgerBusinessRecorder(ledger);

        recorder.scrap(finish("finish-1", "100.000"), "request-1", "破损", new BigDecimal("100.000"), null);

        ArgumentCaptor<InventoryLedgerCommand> captor = ArgumentCaptor.forClass(InventoryLedgerCommand.class);
        verify(ledger).append(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("破损");
        assertThat(captor.getValue().getIdempotencyKey()).isEqualTo("SCRAP:request-1");
    }

    @Test
    void reverseReceiptUsesAdjustmentAndOrderVersionInIdempotencyKey() {
        InventoryLedgerService ledger = mock(InventoryLedgerService.class);
        when(ledger.append(any())).thenReturn(new InventoryLedgerEntry());
        InventoryLedgerBusinessRecorder recorder = new InventoryLedgerBusinessRecorder(ledger);

        recorder.reverseReceipt(finish("finish-1", "100.000"), "order-1", "7", null);

        ArgumentCaptor<InventoryLedgerCommand> captor = ArgumentCaptor.forClass(InventoryLedgerCommand.class);
        verify(ledger).append(captor.capture());
        InventoryLedgerCommand command = captor.getValue();
        assertThat(command.getEventType()).isEqualTo(InventoryLedgerEventType.ADJUSTMENT);
        assertThat(command.getQuantityDelta()).isEqualByComparingTo("-1");
        assertThat(command.getWeightDelta()).isEqualByComparingTo("-100.000");
        assertThat(command.getReservedWeightDelta()).isZero();
        assertThat(command.getReason()).contains("7");
        assertThat(command.getIdempotencyKey()).isEqualTo("REVERSE_RECEIPT:7:finish-1");
    }

    @Test
    void receiptUsesBatchKeyInIdempotencyKey() {
        InventoryLedgerService ledger = mock(InventoryLedgerService.class);
        when(ledger.append(any())).thenReturn(new InventoryLedgerEntry());
        InventoryLedgerBusinessRecorder recorder = new InventoryLedgerBusinessRecorder(ledger);

        recorder.receipt(finish("finish-1", "100.000"), "order-1", "7", null);

        ArgumentCaptor<InventoryLedgerCommand> captor = ArgumentCaptor.forClass(InventoryLedgerCommand.class);
        verify(ledger).append(captor.capture());
        InventoryLedgerCommand command = captor.getValue();
        assertThat(command.getEventType()).isEqualTo(InventoryLedgerEventType.RECEIPT);
        assertThat(command.getSourceBusinessUuid()).isEqualTo("order-1");
        assertThat(command.getIdempotencyKey()).isEqualTo("RECEIPT:7:finish-1");
    }

    @Test
    void receiptAllowsSameFinishInDifferentVersions() {
        InventoryLedgerService ledger = mock(InventoryLedgerService.class);
        when(ledger.append(any())).thenReturn(new InventoryLedgerEntry());
        InventoryLedgerBusinessRecorder recorder = new InventoryLedgerBusinessRecorder(ledger);

        FinishRoll finish = finish("finish-1", "100.000");
        recorder.receipt(finish, "order-1", "7", null);
        recorder.receipt(finish, "order-1", "8", null);

        ArgumentCaptor<InventoryLedgerCommand> captor = ArgumentCaptor.forClass(InventoryLedgerCommand.class);
        verify(ledger, org.mockito.Mockito.times(2)).append(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(InventoryLedgerCommand::getIdempotencyKey)
                .containsExactly("RECEIPT:7:finish-1", "RECEIPT:8:finish-1");
    }

    @Test
    void missingFinishRollIsRejectedAsBusinessError() {
        InventoryLedgerBusinessRecorder recorder = new InventoryLedgerBusinessRecorder(mock(InventoryLedgerService.class));

        assertThatThrownBy(() -> recorder.receipt(null, "order-1", "1", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("finish roll");
    }

    @Test
    void missingReceiptBatchKeyIsRejectedAsBusinessError() {
        InventoryLedgerBusinessRecorder recorder = new InventoryLedgerBusinessRecorder(mock(InventoryLedgerService.class));

        assertThatThrownBy(() -> recorder.receipt(finish("finish-1", "100.000"), "order-1", " ", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("batchKey");
    }

    @Test
    void missingDeliveryDetailVersionIsRejectedAsBusinessError() {
        InventoryLedgerBusinessRecorder recorder = new InventoryLedgerBusinessRecorder(
                mock(InventoryLedgerService.class));

        assertThatThrownBy(() -> recorder.issue(finish("finish-1", "100.000"),
                "delivery-1", "detail-1", new BigDecimal("10.000"), false, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("detail version");
    }

    private FinishRoll finish(String uuid, String actualWeight) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(uuid);
        finish.setActualWeight(new BigDecimal(actualWeight));
        return finish;
    }
}

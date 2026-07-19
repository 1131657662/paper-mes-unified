package com.paper.mes.settle.service;

import com.paper.mes.settle.dto.ReceiveDTO;
import com.paper.mes.settle.entity.ReceiveRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SettleReceiveRequestFingerprintTest {

    @Test
    void sameReceiptPayloadProducesSameFingerprint() {
        ReceiveDTO first = receipt("100.00");
        ReceiveDTO retry = receipt("100.0");

        assertThat(SettleReceiveRequestFingerprint.of(first))
                .isEqualTo(SettleReceiveRequestFingerprint.of(retry));
    }

    @Test
    void changedReceiptAmountProducesDifferentFingerprint() {
        ReceiveDTO first = receipt("100.00");
        ReceiveDTO changed = receipt("101.00");

        assertThat(SettleReceiveRequestFingerprint.of(first))
                .isNotEqualTo(SettleReceiveRequestFingerprint.of(changed));
    }

    @Test
    void legacyRecordWithoutHashMatchesEquivalentRequestWithServerDefaultDate() {
        ReceiveDTO dto = receipt("100.00");
        ReceiveRecord record = new ReceiveRecord();
        record.setReceiveAmount(new BigDecimal("100.00"));
        record.setCashAmount(new BigDecimal("100.00"));
        record.setScrapOffsetAmount(BigDecimal.ZERO);
        record.setDiscountAmount(BigDecimal.ZERO);
        record.setPayMethod(2);
        record.setPayNo("TX-1");
        record.setRemark("cash receipt");

        assertThat(SettleReceiveRequestFingerprint.matchesLegacy(dto, record)).isTrue();
    }

    private ReceiveDTO receipt(String amount) {
        ReceiveDTO dto = new ReceiveDTO();
        dto.setCashAmount(new BigDecimal(amount));
        dto.setPayMethod(2);
        dto.setPayNo("TX-1");
        dto.setRemark("cash receipt");
        return dto;
    }
}

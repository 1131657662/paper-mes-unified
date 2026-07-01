package com.paper.mes.settle.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.settle.entity.SettleDetail;
import com.paper.mes.settle.entity.SettleOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettleAmountSnapshotReaderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolve_whenSnapshotExists_usesSnapshotAmounts() {
        SettleOrder settle = new SettleOrder();
        settle.setSnapBill("""
                {
                  "amount_no_tax": 100.50,
                  "tax_amount": 6.03,
                  "total_amount": 106.53
                }
                """);
        List<SettleDetail> changedDetails = List.of(detail("999.00", "0", "0", "999.00"));

        SettleAmountSnapshotReader.Amounts amounts =
                SettleAmountSnapshotReader.resolve(settle, changedDetails, objectMapper);

        assertEquals(new BigDecimal("100.50"), amounts.noTax());
        assertEquals(new BigDecimal("6.03"), amounts.tax());
        assertEquals(new BigDecimal("106.53"), amounts.total());
    }

    @Test
    void resolve_whenSnapshotMissing_calculatesFromDetails() {
        SettleOrder settle = new SettleOrder();
        List<SettleDetail> details = List.of(
                detail("100.00", "50.00", "10.00", "169.60"),
                detail("20.00", "0", "0", "21.20"));

        SettleAmountSnapshotReader.Amounts amounts =
                SettleAmountSnapshotReader.resolve(settle, details, objectMapper);

        assertEquals(new BigDecimal("180.00"), amounts.noTax());
        assertEquals(new BigDecimal("10.80"), amounts.tax());
        assertEquals(new BigDecimal("190.80"), amounts.total());
    }

    @Test
    void resolve_whenLegacySnapshotMissesAmounts_calculatesFromDetails() {
        SettleOrder settle = new SettleOrder();
        settle.setSnapBill("""
                {
                  "schema_version": "1.0",
                  "snapshot_type": "settle_bill",
                  "details": []
                }
                """);
        List<SettleDetail> details = List.of(
                detail("200.00", "100.00", "30.00", "349.80"),
                detail("0", "60.00", "0", "63.60"));

        SettleAmountSnapshotReader.Amounts amounts =
                SettleAmountSnapshotReader.resolve(settle, details, objectMapper);

        assertEquals(new BigDecimal("390.00"), amounts.noTax());
        assertEquals(new BigDecimal("23.40"), amounts.tax());
        assertEquals(new BigDecimal("413.40"), amounts.total());
    }

    @Test
    void resolve_whenSnapshotMissesTaxAmount_derivesTaxFromTotal() {
        SettleOrder settle = new SettleOrder();
        settle.setSnapBill("""
                {
                  "amountNoTax": 300.00,
                  "totalAmount": 318.00
                }
                """);
        List<SettleDetail> changedDetails = List.of(detail("999.00", "0", "0", "999.00"));

        SettleAmountSnapshotReader.Amounts amounts =
                SettleAmountSnapshotReader.resolve(settle, changedDetails, objectMapper);

        assertEquals(new BigDecimal("300.00"), amounts.noTax());
        assertEquals(new BigDecimal("18.00"), amounts.tax());
        assertEquals(new BigDecimal("318.00"), amounts.total());
    }

    private SettleDetail detail(String saw, String rewind, String extra, String total) {
        SettleDetail detail = new SettleDetail();
        detail.setSawAmount(new BigDecimal(saw));
        detail.setRewindAmount(new BigDecimal(rewind));
        detail.setExtraAmount(new BigDecimal(extra));
        detail.setOrderAmount(new BigDecimal(total));
        return detail;
    }
}

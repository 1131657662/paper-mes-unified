package com.paper.mes.settle.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.settle.dto.SettlePrintLineVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SettleSnapshotPrintLineReaderTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void read_whenPrintLineItemsUseCamelCase_returnsFrozenPrintLines() {
        String snapshot = """
                {
                  "print_line_items": [
                    {
                      "settleUuid": "settle-1",
                      "orderUuid": "order-1",
                      "orderNo": "JG202607010001",
                      "orderDate": "2026-07-01",
                      "originalUuid": "original-1",
                      "originalLabel": "母卷1",
                      "paperName": "牛卡纸",
                      "gramWeight": 450,
                      "originalWidth": 2500,
                      "originalWeight": 3255.000,
                      "processText": "锯纸+复卷",
                      "finishSummary": "A000001、A000002",
                      "finishCount": 2,
                      "finishWeight": 3000.000,
                      "trimWeight": 55.000,
                      "sawUnitPrice": 200.00,
                      "sawInvoiceUnitPrice": 212.00,
                      "rewindUnitPrice": 150.00,
                      "rewindInvoiceUnitPrice": 159.00,
                      "sawAmount": 651.00,
                      "rewindAmount": 359.00,
                      "processAmount": 1010.00,
                      "extraAmount": 100.00,
                      "extraFeeSummary": "装卸费 80.00；运费 20.00",
                      "taxAmount": 66.00,
                      "lineAmount": 1176.00,
                      "isInvoice": 1
                    }
                  ]
                }
                """;

        List<SettlePrintLineVO> lines = SettleSnapshotPrintLineReader.read(snapshot, objectMapper);

        assertNotNull(lines);
        SettlePrintLineVO line = lines.get(0);
        assertEquals("order-1", line.getOrderUuid());
        assertEquals("JG202607010001", line.getOrderNo());
        assertEquals(LocalDate.of(2026, 7, 1), line.getOrderDate());
        assertEquals("母卷1", line.getOriginalLabel());
        assertEquals("牛卡纸", line.getPaperName());
        assertEquals("锯纸+复卷", line.getProcessText());
        assertEquals("A000001、A000002", line.getFinishSummary());
        assertEquals("装卸费 80.00；运费 20.00", line.getExtraFeeSummary());
        assertDecimal("212.00", line.getSawInvoiceUnitPrice());
        assertDecimal("159.00", line.getRewindInvoiceUnitPrice());
        assertDecimal("66.00", line.getTaxAmount());
        assertDecimal("1176.00", line.getLineAmount());
        assertEquals(1, line.getIsInvoice());
    }

    @Test
    void read_whenLegacyPrintLinesUseSnakeCase_returnsFrozenPrintLines() {
        String snapshot = """
                {
                  "print_lines": [
                    {
                      "settle_uuid": "settle-2",
                      "order_uuid": "order-2",
                      "order_no": "JG202607010002",
                      "order_date": "2026-07-02",
                      "original_uuid": "original-2",
                      "original_label": "母卷2",
                      "paper_name": "白卡",
                      "gram_weight": 300,
                      "original_width": 1880,
                      "original_weight": 2874.000,
                      "process_mode": 1,
                      "main_step_type": 2,
                      "process_text": "复卷",
                      "finish_summary": "A000010、A000011",
                      "finish_count": 2,
                      "finish_weight": 2800.000,
                      "trim_weight": 40.000,
                      "saw_weight": 0.000,
                      "rewind_weight": 2874.000,
                      "saw_unit_price": 0.00,
                      "saw_invoice_unit_price": 0.00,
                      "rewind_unit_price": 180.00,
                      "rewind_invoice_unit_price": 190.80,
                      "saw_amount": 0.00,
                      "rewind_amount": 517.32,
                      "process_amount": 517.32,
                      "extra_amount": 30.00,
                      "extra_fee_summary": "其他费 30.00",
                      "tax_amount": 32.84,
                      "line_amount": 580.16,
                      "is_invoice": 1,
                      "remark": "legacy print line"
                    }
                  ]
                }
                """;

        List<SettlePrintLineVO> lines = SettleSnapshotPrintLineReader.read(snapshot, objectMapper);

        assertNotNull(lines);
        SettlePrintLineVO line = lines.get(0);
        assertEquals("settle-2", line.getSettleUuid());
        assertEquals("order-2", line.getOrderUuid());
        assertEquals("JG202607010002", line.getOrderNo());
        assertEquals(LocalDate.of(2026, 7, 2), line.getOrderDate());
        assertEquals("母卷2", line.getOriginalLabel());
        assertEquals("白卡", line.getPaperName());
        assertEquals(300, line.getGramWeight());
        assertEquals(1880, line.getOriginalWidth());
        assertEquals("其他费 30.00", line.getExtraFeeSummary());
        assertDecimal("190.80", line.getRewindInvoiceUnitPrice());
        assertDecimal("517.32", line.getRewindAmount());
        assertDecimal("32.84", line.getTaxAmount());
        assertDecimal("580.16", line.getLineAmount());
        assertEquals("legacy print line", line.getRemark());
    }

    @Test
    void read_whenSnapshotInvalid_returnsNull() {
        assertNull(SettleSnapshotPrintLineReader.read("{bad-json", objectMapper));
    }

    private void assertDecimal(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}

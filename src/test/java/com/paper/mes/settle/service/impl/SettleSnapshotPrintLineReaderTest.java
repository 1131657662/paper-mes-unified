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
                      "originalRollNo": "R001",
                      "originalExtraNo": "C001",
                      "actualGramWeight": 452,
                      "actualWidth": 2498,
                      "originalDiameter": 1300,
                      "coreDiameter": 76,
                      "originalLength": 6000,
                      "originalWeight": 3255.000,
                      "processText": "锯纸+复卷",
                      "processStepSummary": "锯纸（2刀 / 单价 200.00）",
                      "finishSummary": "A000001、A000002",
                      "finishDetailSummary": "A000001（950mm / 1500.000kg）",
                      "finishCount": 2,
                      "finishWeight": 3000.000,
                      "trimWeight": 55.000,
                      "trimSummary": "100mm / 55.000kg",
                      "sawUnitPrice": 200.00,
                      "sawInvoiceUnitPrice": 212.00,
                      "rewindUnitPrice": 150.00,
                      "rewindInvoiceUnitPrice": 159.00,
                      "sawAmount": 651.00,
                      "rewindAmount": 359.00,
                      "processAmount": 1010.00,
                      "extraAmount": 100.00,
                      "extraFeeSummary": "装卸费 80.00；运费 20.00",
                      "taxRate": 6.00,
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
        assertEquals("R001", line.getOriginalRollNo());
        assertEquals("C001", line.getOriginalExtraNo());
        assertEquals(452, line.getActualGramWeight());
        assertEquals(2498, line.getActualWidth());
        assertEquals(1300, line.getOriginalDiameter());
        assertEquals(76, line.getCoreDiameter());
        assertEquals(6000, line.getOriginalLength());
        assertEquals("锯纸+复卷", line.getProcessText());
        assertEquals("锯纸（2刀 / 单价 200.00）", line.getProcessStepSummary());
        assertEquals("A000001、A000002", line.getFinishSummary());
        assertEquals("A000001（950mm / 1500.000kg）", line.getFinishDetailSummary());
        assertEquals("100mm / 55.000kg", line.getTrimSummary());
        assertEquals("装卸费 80.00；运费 20.00", line.getExtraFeeSummary());
        assertDecimal("212.00", line.getSawInvoiceUnitPrice());
        assertDecimal("159.00", line.getRewindInvoiceUnitPrice());
        assertDecimal("6.00", line.getTaxRate());
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
                      "original_roll_no": "R002",
                      "original_extra_no": "C002",
                      "actual_gram_weight": 301,
                      "actual_width": 1878,
                      "original_diameter": 1200,
                      "core_diameter": 76,
                      "original_length": 5800,
                      "original_weight": 2874.000,
                      "process_mode": 1,
                      "main_step_type": 2,
                      "process_text": "复卷",
                      "process_step_summary": "复卷（2874.000kg / 单价 180.00）",
                      "finish_summary": "A000010、A000011",
                      "finish_detail_summary": "A000010（940mm / 1400.000kg）",
                      "finish_count": 2,
                      "finish_weight": 2800.000,
                      "trim_weight": 40.000,
                      "trim_summary": "80mm / 40.000kg",
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
                      "tax_rate": 6.00,
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
        assertEquals("R002", line.getOriginalRollNo());
        assertEquals("C002", line.getOriginalExtraNo());
        assertEquals(301, line.getActualGramWeight());
        assertEquals(1878, line.getActualWidth());
        assertEquals(1200, line.getOriginalDiameter());
        assertEquals(76, line.getCoreDiameter());
        assertEquals(5800, line.getOriginalLength());
        assertEquals("复卷（2874.000kg / 单价 180.00）", line.getProcessStepSummary());
        assertEquals("A000010（940mm / 1400.000kg）", line.getFinishDetailSummary());
        assertEquals("80mm / 40.000kg", line.getTrimSummary());
        assertEquals("其他费 30.00", line.getExtraFeeSummary());
        assertDecimal("190.80", line.getRewindInvoiceUnitPrice());
        assertDecimal("517.32", line.getRewindAmount());
        assertDecimal("6.00", line.getTaxRate());
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

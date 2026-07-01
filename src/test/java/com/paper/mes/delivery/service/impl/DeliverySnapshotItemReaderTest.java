package com.paper.mes.delivery.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DeliverySnapshotItemReaderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void read_whenDetailItemsUseCamelCase_returnsTraceableItems() {
        String snapshot = """
                {
                  "detail_items": [
                    {
                      "finishUuid": "finish-1",
                      "finishRollNo": "A000001",
                      "paperName": "paper-a",
                      "orderNo": "JG202607010001",
                      "gramWeight": 300,
                      "finishWidth": 950,
                      "actualWeight": 121.000,
                      "outWeight": 120.500,
                      "originalSummary": "roll-1 / 2500mm / 3255kg",
                      "processModeText": "rewind",
                      "processSummary": "950x2 + trim",
                      "originalItems": [
                        {
                          "uuid": "original-1",
                          "rollNo": "R001",
                          "paperName": "paper-a",
                          "gramWeight": 300,
                          "originalWidth": 2500,
                          "actualWeight": 3255.000
                        }
                      ],
                      "processStepItems": [
                        {
                          "uuid": "step-1",
                          "originalUuid": "original-1",
                          "stepType": 2,
                          "processWeight": 3.255,
                          "unitPrice": 180.00
                        }
                      ],
                      "actualRemark": "checked"
                    }
                  ]
                }
                """;

        List<DeliveryDetailItemVO> items = DeliverySnapshotItemReader.read(snapshot, objectMapper);

        assertNotNull(items);
        DeliveryDetailItemVO item = items.get(0);
        assertEquals("finish-1", item.getFinishUuid());
        assertEquals("A000001", item.getFinishRollNo());
        assertEquals("JG202607010001", item.getOrderNo());
        assertEquals("roll-1 / 2500mm / 3255kg", item.getOriginalSummary());
        assertEquals("rewind", item.getProcessModeText());
        assertEquals("950x2 + trim", item.getProcessSummary());
        assertEquals("checked", item.getActualRemark());
        assertEquals("R001", item.getOriginalItems().get(0).getRollNo());
        assertEquals("original-1", item.getProcessStepItems().get(0).getOriginalUuid());
        assertEquals(2, item.getProcessStepItems().get(0).getStepType());
        assertEquals(300, item.getGramWeight());
        assertEquals(950, item.getFinishWidth());
        assertEquals(0, new BigDecimal("121.000").compareTo(item.getActualWeight()));
        assertEquals(0, new BigDecimal("120.500").compareTo(item.getOutWeight()));
    }

    @Test
    void read_whenOnlyLegacyDetailsUseSnakeCase_returnsTraceableItems() {
        String snapshot = """
                {
                  "details": [
                    {
                      "finish_uuid": "finish-2",
                      "finish_roll_no": "A000002",
                      "paper_name": "paper-b",
                      "order_no": "JG202607010002",
                      "gram_weight": 450,
                      "finish_width": 1250,
                      "actual_weight": 89.000,
                      "out_weight": 88.000,
                      "original_summary": "roll-2 / 2500mm / 4100kg",
                      "process_mode_text": "saw",
                      "process_summary": "1250x2",
                      "original_items": [
                        {
                          "uuid": "original-2",
                          "roll_no": "R002",
                          "paper_name": "paper-b",
                          "gram_weight": 450,
                          "original_width": 2500,
                          "actual_weight": 4100.000
                        }
                      ],
                      "process_step_items": [
                        {
                          "uuid": "step-2",
                          "original_uuid": "original-2",
                          "step_type": 1,
                          "knife_count": 2,
                          "unit_price": 200.00
                        }
                      ],
                      "actual_remark": "changed loading count"
                    }
                  ]
                }
                """;

        List<DeliveryDetailItemVO> items = DeliverySnapshotItemReader.read(snapshot, objectMapper);

        assertNotNull(items);
        DeliveryDetailItemVO item = items.get(0);
        assertEquals("finish-2", item.getFinishUuid());
        assertEquals("A000002", item.getFinishRollNo());
        assertEquals("JG202607010002", item.getOrderNo());
        assertEquals("roll-2 / 2500mm / 4100kg", item.getOriginalSummary());
        assertEquals("saw", item.getProcessModeText());
        assertEquals("1250x2", item.getProcessSummary());
        assertEquals("changed loading count", item.getActualRemark());
        assertEquals("R002", item.getOriginalItems().get(0).getRollNo());
        assertEquals("step-2", item.getProcessStepItems().get(0).getUuid());
        assertEquals(2, item.getProcessStepItems().get(0).getKnifeCount());
        assertEquals(450, item.getGramWeight());
        assertEquals(1250, item.getFinishWidth());
        assertEquals(0, new BigDecimal("89.000").compareTo(item.getActualWeight()));
        assertEquals(0, new BigDecimal("88.000").compareTo(item.getOutWeight()));
    }

    @Test
    void read_whenSnapshotInvalid_returnsNull() {
        assertNull(DeliverySnapshotItemReader.read("{bad-json", objectMapper));
    }
}

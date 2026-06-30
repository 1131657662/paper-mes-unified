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
    void read_whenDetailItemsUseCamelCase_returnsItems() {
        String snapshot = """
                {
                  "detail_items": [
                    {
                      "finishUuid": "finish-1",
                      "finishRollNo": "A000001",
                      "paperName": "白卡",
                      "outWeight": 120.500
                    }
                  ]
                }
                """;

        List<DeliveryDetailItemVO> items = DeliverySnapshotItemReader.read(snapshot, objectMapper);

        assertNotNull(items);
        assertEquals("finish-1", items.get(0).getFinishUuid());
        assertEquals("A000001", items.get(0).getFinishRollNo());
        assertEquals(0, new BigDecimal("120.500").compareTo(items.get(0).getOutWeight()));
    }

    @Test
    void read_whenOnlyLegacyDetailsUseSnakeCase_returnsItems() {
        String snapshot = """
                {
                  "details": [
                    {
                      "finish_uuid": "finish-2",
                      "finish_roll_no": "A000002",
                      "paper_name": "牛卡",
                      "out_weight": 88.000
                    }
                  ]
                }
                """;

        List<DeliveryDetailItemVO> items = DeliverySnapshotItemReader.read(snapshot, objectMapper);

        assertNotNull(items);
        assertEquals("finish-2", items.get(0).getFinishUuid());
        assertEquals("A000002", items.get(0).getFinishRollNo());
        assertEquals(0, new BigDecimal("88.000").compareTo(items.get(0).getOutWeight()));
    }

    @Test
    void read_whenSnapshotInvalid_returnsNull() {
        assertNull(DeliverySnapshotItemReader.read("{bad-json", objectMapper));
    }
}

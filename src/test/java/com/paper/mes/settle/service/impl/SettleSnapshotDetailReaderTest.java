package com.paper.mes.settle.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.settle.entity.SettleDetail;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettleSnapshotDetailReaderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void read_whenDetailItemsUseCamelCase_returnsFrozenDetails() {
        String snapshot = """
                {
                  "detail_items": [
                    {
                      "uuid": "detail-1",
                      "settleUuid": "settle-1",
                      "orderUuid": "order-1",
                      "orderNo": "JG202607010001",
                      "sawAmount": 651.00,
                      "rewindAmount": 359.00,
                      "extraAmount": 100.00,
                      "orderAmount": 1176.00,
                      "remark": "invoice price locked"
                    }
                  ]
                }
                """;

        List<SettleDetail> details = SettleSnapshotDetailReader.read(snapshot, objectMapper);

        assertNotNull(details);
        SettleDetail detail = details.get(0);
        assertEquals("detail-1", detail.getUuid());
        assertEquals("settle-1", detail.getSettleUuid());
        assertEquals("order-1", detail.getOrderUuid());
        assertEquals("JG202607010001", detail.getOrderNo());
        assertEquals(0, new BigDecimal("651.00").compareTo(detail.getSawAmount()));
        assertEquals(0, new BigDecimal("359.00").compareTo(detail.getRewindAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(detail.getExtraAmount()));
        assertEquals(0, new BigDecimal("1176.00").compareTo(detail.getOrderAmount()));
        assertEquals("invoice price locked", detail.getRemark());
    }

    @Test
    void read_whenOnlyLegacyDetailsUseSnakeCase_returnsFrozenDetails() {
        String snapshot = """
                {
                  "details": [
                    {
                      "uuid": "detail-2",
                      "settle_uuid": "settle-2",
                      "order_uuid": "order-2",
                      "order_no": "JG202607010002",
                      "saw_amount": 300.00,
                      "rewind_amount": 480.00,
                      "extra_amount": 20.00,
                      "order_amount": 848.00,
                      "remark": "legacy snapshot"
                    }
                  ]
                }
                """;

        List<SettleDetail> details = SettleSnapshotDetailReader.read(snapshot, objectMapper);

        assertNotNull(details);
        SettleDetail detail = details.get(0);
        assertEquals("detail-2", detail.getUuid());
        assertEquals("settle-2", detail.getSettleUuid());
        assertEquals("order-2", detail.getOrderUuid());
        assertEquals("JG202607010002", detail.getOrderNo());
        assertEquals(0, new BigDecimal("300.00").compareTo(detail.getSawAmount()));
        assertEquals(0, new BigDecimal("480.00").compareTo(detail.getRewindAmount()));
        assertEquals(0, new BigDecimal("20.00").compareTo(detail.getExtraAmount()));
        assertEquals(0, new BigDecimal("848.00").compareTo(detail.getOrderAmount()));
        assertEquals("legacy snapshot", detail.getRemark());
    }

    @Test
    void read_whenSnapshotAbsent_returnsNullForLegacyCompatibility() {
        assertNull(SettleSnapshotDetailReader.read(null, objectMapper));
    }

    @Test
    void read_whenSnapshotInvalid_rejectsCurrentDetailFallback() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> SettleSnapshotDetailReader.read("{bad-json", objectMapper));

        assertEquals("E008", error.getErrorCode());
    }

    @Test
    void read_whenSnapshotHasNoTraceableDetails_rejectsCurrentDetailFallback() {
        assertThrows(BusinessException.class,
                () -> SettleSnapshotDetailReader.read("{\"detail_items\":[]}", objectMapper));
    }
}

package com.paper.mes.delivery.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryRollbackSnapshotVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeliveryRollbackSnapshotReaderTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void read_whenSnapshotIsNotRollback_returnsNull() {
        assertNull(DeliveryRollbackSnapshotReader.read(
                "{\"snapshot_type\":\"delivery_confirm\"}", objectMapper));
    }

    @Test
    void read_whenPreviousConfirmSnapshotIsValid_returnsFrozenAuditDetails() {
        String snapshot = """
                {
                  "snapshot_type": "delivery_rollback",
                  "rollback_reason": "客户临时改车",
                  "previous_confirm_snapshot": {
                    "delivery_no": "CK202607140001",
                    "detail_items": [{
                      "finishUuid": "finish-1",
                      "finishRollNo": "A000001",
                      "outWeight": 100.000
                    }]
                  }
                }
                """;

        DeliveryRollbackSnapshotVO result = DeliveryRollbackSnapshotReader.read(snapshot, objectMapper);

        assertEquals("CK202607140001", result.getDeliveryNo());
        assertEquals("客户临时改车", result.getRollbackReason());
        assertEquals("A000001", result.getDetails().getFirst().getFinishRollNo());
    }

    @Test
    void read_whenPreviousConfirmSnapshotMissing_rejectsAuditFallback() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> DeliveryRollbackSnapshotReader.read(
                        "{\"snapshot_type\":\"delivery_rollback\"}", objectMapper));

        assertEquals("E008", error.getErrorCode());
    }
}

package com.paper.mes.exporttask.service;

import java.time.LocalDateTime;

public record DeliveryExportSnapshotMetadata(
        String snapshotUuid, String snapshotType, LocalDateTime capturedAt, long rowCount) {

    public static final String INVENTORY_TYPE = "DELIVERY_INVENTORY";
    public static final String RECONCILIATION_TYPE = "DELIVERY_RECONCILIATION";
}

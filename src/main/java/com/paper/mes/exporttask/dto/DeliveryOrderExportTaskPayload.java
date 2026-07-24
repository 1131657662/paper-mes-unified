package com.paper.mes.exporttask.dto;

public record DeliveryOrderExportTaskPayload(
        int schemaVersion,
        int customerRevisionNo,
        String documentFingerprint) {
    public static final int LEGACY_SCHEMA_VERSION = 1;
    public static final int CURRENT_SCHEMA_VERSION = 2;
}

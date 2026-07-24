package com.paper.mes.exporttask.dto;

public record ProcessOrderExportTaskPayload(int schemaVersion, int customerRevisionNo) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
}

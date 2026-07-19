package com.paper.mes.exporttask.service;

import java.time.LocalDateTime;

public record ExportTaskStorageHealth(
        String status,
        boolean available,
        boolean writable,
        long freeBytes,
        long totalBytes,
        double freePercent,
        LocalDateTime checkedAt
) {
    public static final String READY = "READY";
    public static final String UNAVAILABLE = "UNAVAILABLE";
    public static final String READ_ONLY = "READ_ONLY";
    public static final String LOW_SPACE = "LOW_SPACE";
    public static final String ERROR = "ERROR";
}

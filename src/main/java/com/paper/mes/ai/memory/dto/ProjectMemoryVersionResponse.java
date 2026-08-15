package com.paper.mes.ai.memory.dto;

import java.time.LocalDateTime;

public record ProjectMemoryVersionResponse(
        String memoryVersion,
        String schemaVersion,
        String checksum,
        String status,
        String patchNotes,
        String createdBy,
        String approvedBy,
        LocalDateTime createdAt) {
}

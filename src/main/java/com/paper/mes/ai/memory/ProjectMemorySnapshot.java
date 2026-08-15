package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/** Immutable in-process copy of one database-backed project-memory snapshot. */
public record ProjectMemorySnapshot(
        String docVersion,
        String schemaVersion,
        String checksum,
        JsonNode document,
        Instant loadedAt) {

    public ProjectMemorySnapshot {
        if (document == null) {
            throw new IllegalArgumentException("project memory document is required");
        }
        document = document.deepCopy();
    }

    @Override
    public JsonNode document() {
        return document.deepCopy();
    }
}

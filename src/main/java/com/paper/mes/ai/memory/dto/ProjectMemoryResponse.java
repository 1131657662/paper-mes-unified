package com.paper.mes.ai.memory.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ProjectMemoryResponse(
        String memoryVersion,
        String schemaVersion,
        String checksum,
        String state,
        JsonNode document) {
}

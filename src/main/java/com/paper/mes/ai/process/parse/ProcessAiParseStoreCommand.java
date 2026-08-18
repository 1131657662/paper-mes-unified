package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.model.ProcessAiModelResult;

import java.util.List;

public record ProcessAiParseStoreCommand(
        String orderUuid,
        String conversationId,
        int expectedVersion,
        int parseRevision,
        int memoryGeneration,
        String requestIdempotencyKey,
        String status,
        ProjectMemorySnapshot memory,
        List<String> memoryItemIds,
        ProcessAiModelResult modelResult,
        ProcessAiExtractionResult extraction) {

    public ProcessAiParseStoreCommand {
        memoryItemIds = List.copyOf(memoryItemIds);
    }
}

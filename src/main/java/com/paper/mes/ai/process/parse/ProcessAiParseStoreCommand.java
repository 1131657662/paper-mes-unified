package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.intent.ProcessAiExtractionResult;
import com.paper.mes.ai.process.intent.ProcessAiUnderstandingResult;
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
        ProcessAiExtractionResult extraction,
        ProcessAiUnderstandingResult understanding,
        String dialogueState,
        String resultKind,
        int workflowVersion,
        String understandingJson,
        String questionJson,
        String correctionsJson,
        String inputHash,
        String contextHash,
        String previewHash,
        String failureCode,
        String failureTraceId,
        String requiredDefaultIds,
        String acknowledgedDefaultIds,
        String explicitParseId) {

    public ProcessAiParseStoreCommand(
            String orderUuid, String conversationId, int expectedVersion, int parseRevision,
            int memoryGeneration, String requestIdempotencyKey, String status,
            ProjectMemorySnapshot memory, List<String> memoryItemIds,
            ProcessAiModelResult modelResult, ProcessAiExtractionResult extraction) {
        this(orderUuid, conversationId, expectedVersion, parseRevision, memoryGeneration,
                requestIdempotencyKey, status, memory, memoryItemIds, modelResult, extraction,
                null, "PREVIEW_READY", "EXTRACTION", 1, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    public ProcessAiParseStoreCommand(
            String orderUuid, String conversationId, int expectedVersion, int parseRevision,
            int memoryGeneration, String requestIdempotencyKey, String status,
            ProjectMemorySnapshot memory, List<String> memoryItemIds,
            ProcessAiModelResult modelResult, ProcessAiUnderstandingResult understanding,
            String dialogueState, String resultKind, int workflowVersion,
            String questionJson, String inputHash, String contextHash, String previewHash,
            String failureCode, String failureTraceId, String requiredDefaultIds,
            String acknowledgedDefaultIds) {
        this(orderUuid, conversationId, expectedVersion, parseRevision, memoryGeneration,
                requestIdempotencyKey, status, memory, memoryItemIds, modelResult, null,
                understanding, dialogueState, resultKind, workflowVersion, null, questionJson,
                null, inputHash, contextHash, previewHash, failureCode, failureTraceId,
                requiredDefaultIds, acknowledgedDefaultIds, null);
    }

    public String parseId() {
        if (explicitParseId != null) return explicitParseId;
        return extraction != null ? extraction.parseId() : understanding.parseId();
    }

    public ProcessAiParseStoreCommand {
        memoryItemIds = List.copyOf(memoryItemIds);
    }
}

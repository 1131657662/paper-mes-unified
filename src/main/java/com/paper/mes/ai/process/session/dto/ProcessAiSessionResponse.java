package com.paper.mes.ai.process.session.dto;

public record ProcessAiSessionResponse(
        String conversationId,
        String status,
        int currentStep,
        int draftVersion,
        String projectMemoryVersion,
        int memoryGeneration,
        String latestProjectMemoryVersion,
        boolean memoryRefreshAvailable,
        boolean resumed) {
}

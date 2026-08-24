package com.paper.mes.ai.process.session;

record ProcessAiConversationRow(
        String uuid,
        String conversationId,
        String orderUuid,
        String userUuid,
        int currentStep,
        int draftVersion,
        String projectMemoryVersion,
        int memoryGeneration,
        String status,
        int clarificationRound) {

    ProcessAiConversationRow(String uuid, String conversationId, String orderUuid,
                             String userUuid, int currentStep, int draftVersion,
                             String projectMemoryVersion, int memoryGeneration,
                             String status) {
        this(uuid, conversationId, orderUuid, userUuid, currentStep, draftVersion,
                projectMemoryVersion, memoryGeneration, status, 0);
    }
}

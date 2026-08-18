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
        String status) {
}

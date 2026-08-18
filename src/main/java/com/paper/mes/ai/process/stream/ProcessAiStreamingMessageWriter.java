package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.session.ProcessAiMessageService;
import com.paper.mes.ai.process.session.dto.UpdateAssistantMessageCommand;

final class ProcessAiStreamingMessageWriter {

    private static final int FLUSH_CHARACTERS = 256;
    private static final long FLUSH_NANOS = 500_000_000L;
    private static final int MAX_STORED_CHARACTERS = 16_000;

    private final ProcessAiPreparedParse prepared;
    private final ProcessAiMessageService messageService;
    private final StringBuilder content = new StringBuilder();
    private int storedLength;
    private long lastFlush = System.nanoTime();

    ProcessAiStreamingMessageWriter(ProcessAiPreparedParse prepared,
                                    ProcessAiMessageService messageService) {
        this.prepared = prepared;
        this.messageService = messageService;
    }

    void append(String delta) {
        if (delta == null || delta.isEmpty() || content.length() >= MAX_STORED_CHARACTERS) return;
        int remaining = MAX_STORED_CHARACTERS - content.length();
        content.append(delta, 0, Math.min(delta.length(), remaining));
        if (shouldFlush()) flush();
    }

    void flush() {
        if (content.length() == storedLength) return;
        messageService.updateAssistant(new UpdateAssistantMessageCommand(
                prepared.orderUuid(), prepared.request().conversationId(),
                prepared.request().expectedVersion(), prepared.assistantSequence(),
                content.toString(), "PARTIAL", null));
        storedLength = content.length();
        lastFlush = System.nanoTime();
    }

    private boolean shouldFlush() {
        return content.length() - storedLength >= FLUSH_CHARACTERS
                || System.nanoTime() - lastFlush >= FLUSH_NANOS;
    }
}

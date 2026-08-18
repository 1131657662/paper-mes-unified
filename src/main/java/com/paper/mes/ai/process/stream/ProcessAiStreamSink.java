package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;

interface ProcessAiStreamSink {

    default boolean isClosed() {
        return false;
    }

    default void onClosed(Runnable listener) {
    }

    void conversation(String conversationId);

    void delta(String content);

    void result(ProcessAiParseResultResponse result);

    void error(String code, String message, boolean retryable);

    void done();
}

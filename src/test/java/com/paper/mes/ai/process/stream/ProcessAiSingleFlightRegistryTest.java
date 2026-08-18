package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseStreamRequest;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessAiSingleFlightRegistryTest {

    @Test
    void sameLogicalRequestSharesOneFlightAndBroadcastsTheResult() {
        ProcessAiSingleFlightRegistry registry = new ProcessAiSingleFlightRegistry();
        RecordingSink first = new RecordingSink();
        RecordingSink retry = new RecordingSink();

        var owner = registry.register(request("same text"), first);
        var follower = registry.register(request("same text"), retry);
        owner.sink().delta("partial");
        owner.sink().done();

        assertThat(owner.owner()).isTrue();
        assertThat(follower.owner()).isFalse();
        assertThat(first.events).containsExactly("delta:partial", "done");
        assertThat(retry.events).containsExactly("delta:partial", "done");
        assertThat(registry.register(request("same text"), new RecordingSink()).owner()).isTrue();
    }

    @Test
    void sameKeyWithDifferentMessageIsRejected() {
        ProcessAiSingleFlightRegistry registry = new ProcessAiSingleFlightRegistry();
        registry.register(request("first"), new RecordingSink());

        assertThatThrownBy(() -> registry.register(request("different"), new RecordingSink()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "AI_MESSAGE_IDEMPOTENCY_CONFLICT");
    }

    @Test
    void closingLastSubscriberCancelsAndReleasesTheConversation() {
        ProcessAiSingleFlightRegistry registry = new ProcessAiSingleFlightRegistry();
        ClosingSink sink = new ClosingSink();
        var registration = registry.register(request("first"), sink);

        sink.close();

        assertThat(registration.cancellation().isCancelled()).isTrue();
        assertThat(registry.isConversationInFlight("conversation-1")).isFalse();
        assertThat(registry.register(request("first"), new RecordingSink()).owner()).isTrue();
    }

    @Test
    void differentRequestCannotQueueBehindActiveConversation() {
        ProcessAiSingleFlightRegistry registry = new ProcessAiSingleFlightRegistry();
        registry.register(request("first"), new RecordingSink());
        ProcessAiParseStreamRequest next = new ProcessAiParseStreamRequest(
                7, "conversation-1", "request-2", "CLARIFY", "next");

        assertThatThrownBy(() -> registry.register(next, new RecordingSink()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "AI_CONVERSATION_BUSY");
    }

    private ProcessAiParseStreamRequest request(String message) {
        return new ProcessAiParseStreamRequest(
                7, "conversation-1", "request-1", "START", message);
    }

    private static final class RecordingSink implements ProcessAiStreamSink {
        private final List<String> events = new ArrayList<>();

        @Override public void conversation(String value) { events.add("conversation:" + value); }
        @Override public void delta(String value) { events.add("delta:" + value); }
        @Override public void result(ProcessAiParseResultResponse value) { events.add("result"); }
        @Override public void error(String code, String message, boolean retryable) { events.add("error:" + code); }
        @Override public void done() { events.add("done"); }
    }

    private static final class ClosingSink implements ProcessAiStreamSink {
        private Runnable closeListener;

        void close() { closeListener.run(); }
        @Override public void onClosed(Runnable listener) { closeListener = listener; }
        @Override public void conversation(String value) { }
        @Override public void delta(String value) { }
        @Override public void result(ProcessAiParseResultResponse value) { }
        @Override public void error(String code, String message, boolean retryable) { }
        @Override public void done() { }
    }
}

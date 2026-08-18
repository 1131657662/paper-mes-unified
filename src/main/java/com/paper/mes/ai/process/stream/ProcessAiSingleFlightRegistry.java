package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.model.ProcessAiCancellation;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseStreamRequest;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ProcessAiSingleFlightRegistry {

    private final Map<FlightKey, Flight> flights = new LinkedHashMap<>();

    synchronized Registration register(ProcessAiParseStreamRequest request,
                                       ProcessAiStreamSink subscriber) {
        FlightKey key = new FlightKey(request.conversationId(), request.idempotencyKey());
        Flight current = flights.get(key);
        if (current != null) {
            current.requireSameRequest(request);
            current.add(subscriber);
            return new Registration(false, current, current.cancellation);
        }
        boolean conversationBusy = flights.keySet().stream()
                .anyMatch(existing -> existing.conversationId().equals(request.conversationId()));
        if (conversationBusy) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "AI_CONVERSATION_BUSY", "当前会话已有 AI 请求正在处理");
        }
        Flight created = new Flight(key, request);
        flights.put(key, created);
        created.add(subscriber);
        if (created.cancellation.isCancelled()) flights.remove(key, created);
        return new Registration(true, created, created.cancellation);
    }

    public synchronized boolean isConversationInFlight(String conversationId) {
        return flights.keySet().stream()
                .anyMatch(key -> key.conversationId().equals(conversationId));
    }

    record Registration(boolean owner, ProcessAiStreamSink sink, ProcessAiCancellation cancellation) {
    }

    private record FlightKey(String conversationId, String idempotencyKey) {
    }

    private final class Flight implements ProcessAiStreamSink {
        private final FlightKey key;
        private final ProcessAiParseStreamRequest request;
        private final List<ProcessAiStreamSink> subscribers = new CopyOnWriteArrayList<>();
        private final ProcessAiCancellation cancellation = new ProcessAiCancellation();
        private final AtomicBoolean finished = new AtomicBoolean();

        private Flight(FlightKey key, ProcessAiParseStreamRequest request) {
            this.key = key;
            this.request = request;
        }

        private void requireSameRequest(ProcessAiParseStreamRequest incoming) {
            if (!request.equals(incoming)) {
                throw new BusinessException(ResultCode.CONFLICT,
                        "AI_MESSAGE_IDEMPOTENCY_CONFLICT", "该请求标识已用于不同消息");
            }
        }

        private void add(ProcessAiStreamSink subscriber) {
            subscribers.add(subscriber);
            subscriber.onClosed(() -> remove(subscriber));
        }

        private void remove(ProcessAiStreamSink subscriber) {
            subscribers.remove(subscriber);
            if (!subscribers.isEmpty() || finished.get()) return;
            cancellation.cancel();
            synchronized (ProcessAiSingleFlightRegistry.this) {
                flights.remove(key, this);
            }
        }

        @Override
        public void conversation(String conversationId) {
            subscribers.forEach(value -> value.conversation(conversationId));
        }

        @Override
        public void delta(String content) {
            subscribers.forEach(value -> value.delta(content));
        }

        @Override
        public void result(ProcessAiParseResultResponse result) {
            subscribers.forEach(value -> value.result(result));
        }

        @Override
        public void error(String code, String message, boolean retryable) {
            subscribers.forEach(value -> value.error(code, message, retryable));
        }

        @Override
        public void done() {
            if (!finished.compareAndSet(false, true)) return;
            subscribers.forEach(ProcessAiStreamSink::done);
            subscribers.clear();
            synchronized (ProcessAiSingleFlightRegistry.this) {
                flights.remove(key, this);
            }
        }
    }
}

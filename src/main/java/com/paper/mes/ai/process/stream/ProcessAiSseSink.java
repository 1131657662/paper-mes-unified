package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

final class ProcessAiSseSink implements ProcessAiStreamSink {

    private final SseEmitter emitter;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final List<Runnable> closeListeners = new CopyOnWriteArrayList<>();

    ProcessAiSseSink(long timeoutMs) {
        emitter = new SseEmitter(timeoutMs);
        emitter.onCompletion(this::markClosed);
        emitter.onTimeout(this::markClosed);
        emitter.onError(ignored -> markClosed());
    }

    SseEmitter emitter() {
        return emitter;
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void onClosed(Runnable listener) {
        closeListeners.add(listener);
        if (closed.get() && closeListeners.remove(listener)) listener.run();
    }

    void heartbeat() {
        sendEvent(SseEmitter.event().comment("keepalive"));
    }

    @Override
    public void conversation(String conversationId) {
        send("conversation", Map.of("conversationId", conversationId));
    }

    @Override
    public void delta(String content) {
        send("delta", Map.of("content", content));
    }

    @Override
    public void result(ProcessAiParseResultResponse result) {
        send("result", result);
    }

    @Override
    public void error(String code, String message, boolean retryable) {
        if (closed.get()) return;
        send("error", Map.of("code", code, "message", message, "retryable", retryable));
    }

    @Override
    public void done() {
        if (closed.get()) return;
        sendEvent(SseEmitter.event().name("done").data(Map.of("done", true)));
        if (markClosed()) {
            emitter.complete();
        }
    }

    private void send(String event, Object data) {
        sendEvent(SseEmitter.event().name(event).data(data));
    }

    private void sendEvent(SseEmitter.SseEventBuilder event) {
        if (closed.get()) return;
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException ex) {
            markClosed();
        }
    }

    private boolean markClosed() {
        if (!closed.compareAndSet(false, true)) return false;
        closeListeners.forEach(Runnable::run);
        closeListeners.clear();
        return true;
    }
}

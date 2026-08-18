package com.paper.mes.ai.process.stream;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Component
class ProcessAiHeartbeatScheduler {

    private static final long HEARTBEAT_SECONDS = 15L;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "process-ai-heartbeat-" + ThreadNumber.next());
        thread.setDaemon(true);
        return thread;
    });

    void register(ProcessAiSseSink sink) {
        AtomicReference<ScheduledFuture<?>> reference = new AtomicReference<>();
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> heartbeat(sink, reference), HEARTBEAT_SECONDS,
                HEARTBEAT_SECONDS, TimeUnit.SECONDS);
        reference.set(future);
        sink.onClosed(() -> future.cancel(false));
    }

    private void heartbeat(ProcessAiSseSink sink,
                           AtomicReference<ScheduledFuture<?>> reference) {
        if (sink.isClosed()) {
            ScheduledFuture<?> future = reference.get();
            if (future != null) future.cancel(false);
            return;
        }
        sink.heartbeat();
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }

    private static final class ThreadNumber {
        private static final AtomicInteger VALUE = new AtomicInteger();

        private static int next() {
            return VALUE.incrementAndGet();
        }
    }
}

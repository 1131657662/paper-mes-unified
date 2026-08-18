package com.paper.mes.ai.process.model;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** Cooperative cancellation shared by the SSE flight, worker and provider clients. */
public final class ProcessAiCancellation {

    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    public boolean cancel() {
        if (!cancelled.compareAndSet(false, true)) return false;
        listeners.forEach(ProcessAiCancellation::runQuietly);
        listeners.clear();
        return true;
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public Registration onCancel(Runnable listener) {
        if (cancelled.get()) {
            runQuietly(listener);
            return () -> { };
        }
        listeners.add(listener);
        if (cancelled.get() && listeners.remove(listener)) runQuietly(listener);
        return () -> listeners.remove(listener);
    }

    public void throwIfCancelled() {
        if (cancelled.get()) {
            throw new ProcessAiProviderException(
                    "AI_REQUEST_CANCELLED", false, "AI request was cancelled by the client");
        }
    }

    private static void runQuietly(Runnable listener) {
        try {
            listener.run();
        } catch (RuntimeException ignored) {
            // Cancellation remains best-effort for each registered resource.
        }
    }

    @FunctionalInterface
    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }
}

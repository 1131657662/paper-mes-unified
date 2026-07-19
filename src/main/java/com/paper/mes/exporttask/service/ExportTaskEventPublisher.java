package com.paper.mes.exporttask.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ExportTaskEventPublisher {
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    public ExportTaskEventPublisher(MeterRegistry meterRegistry) {
        Gauge.builder("paper_mes_export_sse_connections", subscribers, ExportTaskEventPublisher::countConnections)
                .description("Active export task SSE connections")
                .register(meterRegistry);
    }

    public SseEmitter subscribe(String userUuid) {
        SseEmitter emitter = new SseEmitter(0L);
        subscribers.computeIfAbsent(userUuid, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable cleanup = () -> remove(userUuid, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        if (!send(emitter, "connected", Map.of("type", "connected"))) cleanup.run();
        return emitter;
    }

    public void publish(String userUuid, String taskUuid, Integer taskStatus) {
        List<SseEmitter> emitters = subscribers.get(userUuid);
        if (emitters == null) return;
        Map<String, Object> payload = Map.of("taskUuid", taskUuid, "taskStatus", taskStatus);
        emitters.removeIf(emitter -> !send(emitter, "export-task", payload));
        removeEmpty(userUuid, emitters);
    }

    public void publishRefresh() {
        publishToAll("refresh", Map.of("type", "refresh"));
    }

    public void publishRefresh(String userUuid) {
        List<SseEmitter> emitters = subscribers.get(userUuid);
        if (emitters == null) return;
        emitters.removeIf(emitter -> !send(emitter, "refresh", Map.of("type", "refresh")));
        removeEmpty(userUuid, emitters);
    }

    public Set<String> subscriberUserUuids() {
        return Set.copyOf(subscribers.keySet());
    }

    public long connectionCount() {
        return Math.round(countConnections(subscribers));
    }

    @Scheduled(fixedDelayString = "${app.export-task.sse-heartbeat-ms:25000}")
    public void heartbeat() {
        publishToAll("heartbeat", Map.of("type", "heartbeat"));
    }

    private void publishToAll(String name, Object payload) {
        subscribers.forEach((userUuid, emitters) -> {
            emitters.removeIf(emitter -> !send(emitter, name, payload));
            removeEmpty(userUuid, emitters);
        });
    }

    private boolean send(SseEmitter emitter, String name, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(name).data(payload));
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private void remove(String userUuid, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = subscribers.get(userUuid);
        if (emitters == null) return;
        emitters.remove(emitter);
        removeEmpty(userUuid, emitters);
    }

    private void removeEmpty(String userUuid, List<SseEmitter> emitters) {
        if (emitters.isEmpty()) subscribers.remove(userUuid, emitters);
    }

    private static double countConnections(Map<String, CopyOnWriteArrayList<SseEmitter>> entries) {
        return entries.values().stream().mapToInt(List::size).sum();
    }
}

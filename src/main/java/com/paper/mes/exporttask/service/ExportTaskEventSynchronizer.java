package com.paper.mes.exporttask.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ExportTaskEventSynchronizer {
    private final ExportTaskEventPublisher publisher;
    private final ExportTaskEventRevisionReader revisionReader;
    private final Map<String, String> seenRevisions = new ConcurrentHashMap<>();
    private final AtomicBoolean databaseAvailable = new AtomicBoolean(true);

    public ExportTaskEventSynchronizer(ExportTaskEventPublisher publisher,
                                       ExportTaskEventRevisionReader revisionReader,
                                       MeterRegistry meterRegistry) {
        this.publisher = publisher;
        this.revisionReader = revisionReader;
        Gauge.builder("paper_mes_export_event_sync_available", databaseAvailable,
                        available -> available.get() ? 1 : 0)
                .description("Cross-instance export task event synchronization availability")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.export-task.sse-sync-delay-ms:2000}")
    public void synchronize() {
        Set<String> users = publisher.subscriberUserUuids();
        if (users.isEmpty()) {
            seenRevisions.clear();
            return;
        }
        try {
            Map<String, String> revisions = revisionReader.read(users);
            seenRevisions.keySet().removeIf(userUuid -> !users.contains(userUuid));
            users.forEach(userUuid -> publishIfChanged(userUuid, revisions.get(userUuid)));
            markDatabaseAvailable();
        } catch (RuntimeException exception) {
            markDatabaseUnavailable(exception);
        }
    }

    private void publishIfChanged(String userUuid, String currentRevision) {
        String revision = currentRevision == null ? "0:0" : currentRevision;
        String previous = seenRevisions.put(userUuid, revision);
        if (previous == null || !previous.equals(revision)) publisher.publishRefresh(userUuid);
    }

    private void markDatabaseAvailable() {
        if (databaseAvailable.compareAndSet(false, true)) {
            log.info("Export task cross-instance event synchronization recovered");
        }
    }

    private void markDatabaseUnavailable(RuntimeException exception) {
        if (databaseAvailable.compareAndSet(true, false)) {
            log.warn("Export task cross-instance event synchronization unavailable; frontend polling remains active",
                    exception);
        }
    }
}

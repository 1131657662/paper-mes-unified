package com.paper.mes.report.config;

import com.paper.mes.report.service.ReportQuerySnapshotStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportQuerySnapshotCleanupScheduler {
    private final ReportQuerySnapshotStore store;

    @Value("${app.report.query-snapshot-cleanup-batch-size:500}")
    private int batchSize;

    @Scheduled(cron = "${app.report.query-snapshot-cleanup-cron:0 15 * * * *}")
    public void cleanup() {
        try {
            int deleted = store.cleanupExpired(LocalDateTime.now(), batchSize);
            if (deleted > 0) log.info("Cleaned {} expired report query snapshots", deleted);
        } catch (RuntimeException exception) {
            log.error("Scheduled report query snapshot cleanup failed", exception);
        }
    }
}

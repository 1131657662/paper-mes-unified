package com.paper.mes.report.materialization.config;

import com.paper.mes.report.materialization.service.ReportSnapshotCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportSnapshotCleanupScheduler {
    private final ReportSnapshotCleanupService cleanupService;
    @Value("${app.report.snapshot-retired-retention-days:90}")
    private int retiredRetentionDays;
    @Value("${app.report.snapshot-cleanup-batch-size:100}")
    private int batchSize;

    @Scheduled(cron = "${app.report.snapshot-cleanup-cron:0 45 3 * * *}")
    public void cleanup() {
        try {
            int deleted = cleanupService.cleanup(retiredRetentionDays, batchSize);
            if (deleted > 0) log.info("Cleaned {} expired report snapshots", deleted);
        } catch (RuntimeException exception) {
            log.error("Scheduled report snapshot cleanup failed", exception);
        }
    }
}

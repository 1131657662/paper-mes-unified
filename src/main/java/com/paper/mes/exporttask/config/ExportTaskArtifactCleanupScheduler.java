package com.paper.mes.exporttask.config;

import com.paper.mes.exporttask.service.ExportTaskMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportTaskArtifactCleanupScheduler {
    private final ExportTaskMaintenanceService maintenanceService;
    private final ExportTaskRuntimeProperties runtimeProperties;

    @Scheduled(
            initialDelayString = "${app.export-task.orphan-cleanup-initial-delay-ms:60000}",
            fixedDelayString = "${app.export-task.orphan-cleanup-delay-ms:900000}")
    public void cleanup() {
        try {
            LocalDateTime cutoff = LocalDateTime.now()
                    .minusMinutes(runtimeProperties.getOrphanRetentionMinutes());
            int deleted = maintenanceService.cleanupOrphanArtifacts(
                    cutoff, runtimeProperties.getOrphanScanLimit());
            if (deleted > 0) log.info("Export task orphan artifacts deleted: {}", deleted);
        } catch (RuntimeException exception) {
            log.error("Export task orphan artifact cleanup failed", exception);
        }
    }
}

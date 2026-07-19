package com.paper.mes.exporttask.config;

import com.paper.mes.exporttask.service.ExportTaskStorageAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportTaskStorageAlertScheduler {
    private final ExportTaskStorageAlertService alertService;

    @Scheduled(
            initialDelayString = "${app.export-task.storage-alert-initial-delay-ms:30000}",
            fixedDelayString = "${app.export-task.storage-alert-check-delay-ms:60000}")
    public void check() {
        try {
            alertService.check();
        } catch (RuntimeException exception) {
            log.error("Export task storage alert check failed", exception);
        }
    }
}

package com.paper.mes.exporttask.config;

import com.paper.mes.exporttask.service.ExportTaskMaintenanceService;
import com.paper.mes.exporttask.service.ExportTaskEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportTaskMaintenanceScheduler {
    private final ExportTaskMaintenanceService maintenanceService;
    private final ExportTaskEventPublisher eventPublisher;
    private final ExportTaskRuntimeProperties runtimeProperties;

    @Scheduled(
            initialDelayString = "${app.export-task.initial-delay-ms:5000}",
            fixedDelayString = "${app.export-task.poll-delay-ms:10000}")
    public void maintain() {
        try {
            LocalDateTime now = LocalDateTime.now();
            int recovered = maintenanceService.recoverStaleRunning(now.minusMinutes(runtimeProperties.getStaleMinutes()));
            int exhausted = maintenanceService.failExhaustedQueued();
            int expired = maintenanceService.expireCompleted(now);
            int dispatched = maintenanceService.dispatchPending();
            if (recovered + exhausted + expired + dispatched > 0) {
                eventPublisher.publishRefresh();
                log.info("Export task maintenance: recovered={}, exhausted={}, expired={}, dispatched={}",
                        recovered, exhausted, expired, dispatched);
            }
        } catch (RuntimeException exception) {
            log.error("Export task maintenance failed", exception);
        }
    }
}

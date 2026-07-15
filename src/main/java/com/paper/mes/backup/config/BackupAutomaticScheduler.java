package com.paper.mes.backup.config;

import com.paper.mes.backup.service.BackupAutomaticCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupAutomaticScheduler {

    private final BackupAutomaticCoordinator coordinator;

    @Scheduled(fixedDelayString = "${app.backup.automatic-check-delay:60000}")
    public void runDueBackup() {
        try {
            coordinator.runDueBackup();
        } catch (RuntimeException ex) {
            log.error("Automatic backup scheduling failed", ex);
        }
    }
}

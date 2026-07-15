package com.paper.mes.backup.config;

import com.paper.mes.backup.service.BackupMaintenanceService;
import com.paper.mes.backup.service.BackupTaskExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
public class BackupCleanupScheduler {

    private final BackupMaintenanceService maintenanceService;
    private final BackupTaskExecutor taskExecutor;

    public BackupCleanupScheduler(BackupMaintenanceService maintenanceService,
                                  BackupTaskExecutor taskExecutor) {
        this.maintenanceService = maintenanceService;
        this.taskExecutor = taskExecutor;
    }

    @Scheduled(cron = "${app.backup.cleanup-cron:0 15 3 * * *}")
    public void cleanupExpiredBackups() {
        if (taskExecutor.isRunning()) return;
        try {
            int deleted = maintenanceService.cleanupExpired("system");
            if (deleted > 0) log.info("Deleted {} expired backups", deleted);
        } catch (RuntimeException ex) {
            log.error("Scheduled backup cleanup failed", ex);
        }
    }
}

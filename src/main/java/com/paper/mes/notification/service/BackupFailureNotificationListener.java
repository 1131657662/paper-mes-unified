package com.paper.mes.notification.service;

import com.paper.mes.backup.service.BackupTaskFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupFailureNotificationListener {

    private final SystemNotificationService notificationService;

    @EventListener
    public void onBackupTaskFailed(BackupTaskFailedEvent event) {
        try {
            notificationService.publishBackupFailure(event);
        } catch (RuntimeException ex) {
            log.error("Failed to publish backup failure notification for task {}", event.taskUuid(), ex);
        }
    }
}

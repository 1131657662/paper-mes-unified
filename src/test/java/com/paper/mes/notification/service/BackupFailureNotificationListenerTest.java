package com.paper.mes.notification.service;

import com.paper.mes.backup.service.BackupTaskFailedEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class BackupFailureNotificationListenerTest {

    @Test
    void onBackupTaskFailed_whenNotificationStorageFails_doesNotInterruptTaskHandling() {
        SystemNotificationService service = mock(SystemNotificationService.class);
        BackupTaskFailedEvent event = new BackupTaskFailedEvent(
                "task-uuid", "BACKUP", null, LocalDateTime.now());
        doThrow(new IllegalStateException("database unavailable"))
                .when(service).publishBackupFailure(event);
        BackupFailureNotificationListener listener = new BackupFailureNotificationListener(service);

        assertDoesNotThrow(() -> listener.onBackupTaskFailed(event));
    }
}

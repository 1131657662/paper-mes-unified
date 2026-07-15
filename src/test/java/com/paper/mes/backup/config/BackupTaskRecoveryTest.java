package com.paper.mes.backup.config;

import com.paper.mes.backup.service.BackupTaskHistoryService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackupTaskRecoveryTest {

    @Test
    void run_afterSchemaBootstrap_recoversInterruptedTasks() {
        BackupTaskHistoryService historyService = mock(BackupTaskHistoryService.class);
        when(historyService.recoverInterruptedTasks()).thenReturn(1);
        BackupTaskRecovery recovery = new BackupTaskRecovery(historyService);

        recovery.run(null);

        verify(historyService).recoverInterruptedTasks();
    }
}

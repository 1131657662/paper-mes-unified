package com.paper.mes.backup.service;

import org.junit.jupiter.api.Test;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackupAutomaticCoordinatorTest {

    @Test
    void runDueBackup_whenTaskAlreadyRunning_doesNotSubmit() {
        BackupAutomaticPolicy policy = mock(BackupAutomaticPolicy.class);
        BackupTaskExecutor executor = mock(BackupTaskExecutor.class);
        when(policy.isDue(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(executor.isRunning()).thenReturn(true);
        BackupAutomaticCoordinator coordinator = new BackupAutomaticCoordinator(policy, executor);

        coordinator.runDueBackup();

        verify(executor, never()).startAutomaticBackup();
    }

    @Test
    void runDueBackup_whenDue_startsAutomaticBackup() {
        BackupAutomaticPolicy policy = mock(BackupAutomaticPolicy.class);
        BackupTaskExecutor executor = mock(BackupTaskExecutor.class);
        when(policy.isDue(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        BackupAutomaticCoordinator coordinator = new BackupAutomaticCoordinator(policy, executor);

        coordinator.runDueBackup();

        verify(executor).startAutomaticBackup();
    }

    @Test
    void runDueBackup_whenOperationLockIsBusy_skipsCurrentCheck() {
        BackupAutomaticPolicy policy = mock(BackupAutomaticPolicy.class);
        BackupTaskExecutor executor = mock(BackupTaskExecutor.class);
        when(policy.isDue(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        doThrow(new BusinessException(ResultCode.CONFLICT, "busy"))
                .when(executor).startAutomaticBackup();
        BackupAutomaticCoordinator coordinator = new BackupAutomaticCoordinator(policy, executor);

        coordinator.runDueBackup();

        verify(executor).startAutomaticBackup();
    }
}

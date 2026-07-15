package com.paper.mes.backup.service;

import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.backup.dto.BackupRecordVO;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BackupTaskExecutorTest {

    @Test
    void startBackup_whenAnotherTaskIsRunning_rejectsConcurrentStart() throws Exception {
        BackupCommandRunner runner = mock(BackupCommandRunner.class);
        BackupCatalog catalog = mock(BackupCatalog.class);
        BackupTaskHistoryService history = mock(BackupTaskHistoryService.class);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        when(catalog.root()).thenReturn(Path.of("backup"));
        when(history.start("BACKUP", null, "admin")).thenReturn("task-uuid");
        doAnswer(invocation -> {
            started.countDown();
            release.await(2, TimeUnit.SECONDS);
            return null;
        }).when(runner).backup(any(Path.class));
        BackupTaskExecutor executor = new BackupTaskExecutor(runner, catalog, history,
                mock(OperationLogService.class), new BackupOperationGuard());

        try {
            executor.startBackup("admin");
            assertTrue(started.await(1, TimeUnit.SECONDS));

            BusinessException error = assertThrows(BusinessException.class,
                    () -> executor.startBackup("other-admin"));
            assertEquals("已有备份任务正在执行", error.getMessage());
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    @Test
    void startBackup_whenRunnerFails_recordsGenericFailureMessage() {
        BackupCommandRunner runner = mock(BackupCommandRunner.class);
        BackupCatalog catalog = mock(BackupCatalog.class);
        BackupTaskHistoryService history = mock(BackupTaskHistoryService.class);
        when(catalog.root()).thenReturn(Path.of("backup"));
        when(history.start("BACKUP", null, "admin")).thenReturn("task-uuid");
        doThrow(new IllegalStateException("C:\\private\\backup.env"))
                .when(runner).backup(any(Path.class));
        BackupTaskExecutor executor = new BackupTaskExecutor(runner, catalog, history,
                mock(OperationLogService.class), new BackupOperationGuard());

        try {
            executor.startBackup("admin");

            verify(history, timeout(2000)).finish(eq("task-uuid"), isNull(), eq(false),
                    eq("任务失败，请查看服务器日志"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void startBackup_whenRunnerTimesOut_recordsSafeFailureReason() {
        BackupCommandRunner runner = mock(BackupCommandRunner.class);
        BackupCatalog catalog = mock(BackupCatalog.class);
        BackupTaskHistoryService history = mock(BackupTaskHistoryService.class);
        when(catalog.root()).thenReturn(Path.of("backup"));
        when(history.start("BACKUP", null, "admin")).thenReturn("task-uuid");
        doThrow(new IllegalStateException("备份任务执行超时，内部路径 C:\\private\\backup.env"))
                .when(runner).backup(any(Path.class));
        BackupTaskExecutor executor = new BackupTaskExecutor(runner, catalog, history,
                mock(OperationLogService.class), new BackupOperationGuard());

        try {
            executor.startBackup("admin");

            verify(history, timeout(2000)).finish(eq("task-uuid"), isNull(), eq(false),
                    eq("任务执行超时，请检查备份脚本和数据库连接"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void startBackup_whenRunnerCannotStart_recordsSafeFailureReason() {
        BackupCommandRunner runner = mock(BackupCommandRunner.class);
        BackupCatalog catalog = mock(BackupCatalog.class);
        BackupTaskHistoryService history = mock(BackupTaskHistoryService.class);
        when(catalog.root()).thenReturn(Path.of("backup"));
        when(history.start("BACKUP", null, "admin")).thenReturn("task-uuid");
        doThrow(new IllegalStateException("无法启动 PowerShell 脚本 C:\\private\\backup.ps1"))
                .when(runner).backup(any(Path.class));
        BackupTaskExecutor executor = new BackupTaskExecutor(runner, catalog, history,
                mock(OperationLogService.class), new BackupOperationGuard());

        try {
            executor.startBackup("admin");

            verify(history, timeout(2000)).finish(eq("task-uuid"), isNull(), eq(false),
                    eq("备份脚本无法启动，请检查脚本路径和运行权限"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void startBackup_whenRunnerExitsWithError_recordsSafeFailureReason() {
        BackupCommandRunner runner = mock(BackupCommandRunner.class);
        BackupCatalog catalog = mock(BackupCatalog.class);
        BackupTaskHistoryService history = mock(BackupTaskHistoryService.class);
        when(catalog.root()).thenReturn(Path.of("backup"));
        when(history.start("BACKUP", null, "admin")).thenReturn("task-uuid");
        doThrow(new IllegalStateException("备份脚本执行失败，退出码 1，路径 C:\\private\\backup.ps1"))
                .when(runner).backup(any(Path.class));
        BackupTaskExecutor executor = new BackupTaskExecutor(runner, catalog, history,
                mock(OperationLogService.class), new BackupOperationGuard());

        try {
            executor.startBackup("admin");

            verify(history, timeout(2000)).finish(eq("task-uuid"), isNull(), eq(false),
                    eq("备份脚本执行失败，请查看任务日志"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void startAutomaticBackup_whenRunnerSucceeds_recordsAutomaticTask() {
        BackupCommandRunner runner = mock(BackupCommandRunner.class);
        BackupCatalog catalog = mock(BackupCatalog.class);
        BackupTaskHistoryService history = mock(BackupTaskHistoryService.class);
        when(catalog.root()).thenReturn(Path.of("backup"));
        when(catalog.list()).thenReturn(List.of(BackupRecordVO.builder()
                .id("20260713-023500").createdAt(LocalDateTime.now()).build()));
        when(history.start("AUTO_BACKUP", null, "system")).thenReturn("task-uuid");
        BackupTaskExecutor executor = new BackupTaskExecutor(runner, catalog, history,
                mock(OperationLogService.class), new BackupOperationGuard());

        try {
            executor.startAutomaticBackup();

            verify(history, timeout(2000)).finish("task-uuid", "20260713-023500",
                    true, "自动备份完成");
        } finally {
            executor.shutdown();
        }
    }
}

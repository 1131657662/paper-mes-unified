package com.paper.mes.exporttask.service;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.exporttask.entity.ExportTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportTaskWorkerTest {
    private ExportTaskExecutionLeaseService leaseService;
    private ExportTaskStorage storage;
    private ExportTaskHandler handler;
    private ExportTaskExecutionAuthorizer authorizer;
    private ExportTaskEventPublisher eventPublisher;
    private ExportTaskHeartbeat heartbeat;
    private ExportTaskExecutionLease lease;
    private ExportTaskWorker worker;

    @BeforeEach
    void setUp() {
        leaseService = mock(ExportTaskExecutionLeaseService.class);
        storage = mock(ExportTaskStorage.class);
        handler = mock(ExportTaskHandler.class);
        authorizer = mock(ExportTaskExecutionAuthorizer.class);
        eventPublisher = mock(ExportTaskEventPublisher.class);
        heartbeat = mock(ExportTaskHeartbeat.class);
        lease = new ExportTaskExecutionLease(queuedTask(), "lease-1");
        when(handler.taskType()).thenReturn(ReportExportTaskHandler.TASK_TYPE);
        when(handler.requiredPermission()).thenReturn(Permissions.REPORT_VIEW);
        when(handler.fileExtension()).thenReturn("xlsx");
        when(leaseService.claim(lease.task().getUuid())).thenReturn(Optional.of(lease));
        when(leaseService.startHeartbeat(lease)).thenReturn(heartbeat);
        worker = new ExportTaskWorker(leaseService, storage,
                new ExportTaskHandlerRegistry(List.of(handler)), authorizer, eventPublisher);
    }

    @Test
    void execute_whenPermissionRevokedBeforeStart_doesNotGenerateArtifact() throws Exception {
        ExportTask task = lease.task();
        when(authorizer.canExecute(task, Permissions.REPORT_VIEW)).thenReturn(false);
        when(leaseService.markFailure(lease, "账号已停用或业务权限已变更，任务已终止")).thenReturn(true);

        worker.execute(task.getUuid());

        verify(handler, never()).generate(any(), any());
        verify(storage, never()).target(any(), any());
        verify(eventPublisher).publish("user-1", "task-1", 4);
        verify(heartbeat).close();
    }

    @Test
    void execute_whenPermissionRevokedDuringGeneration_deletesArtifactWithoutPublishingSuccess() throws Exception {
        ExportTask task = lease.task();
        Path target = Path.of("build", "test-export-task.xlsx");
        when(authorizer.canExecute(task, Permissions.REPORT_VIEW)).thenReturn(true, false);
        when(storage.target(lease, "xlsx")).thenReturn(target);
        when(handler.generate(task, target)).thenReturn(new ExportTaskArtifact("report.xlsx", "application/xlsx"));
        when(leaseService.markFailure(lease, "账号已停用或业务权限已变更，任务已终止")).thenReturn(true);

        worker.execute(task.getUuid());

        verify(storage).delete(target);
        InOrder events = inOrder(eventPublisher);
        events.verify(eventPublisher).publish("user-1", "task-1", 2);
        events.verify(eventPublisher).publish("user-1", "task-1", 4);
        verify(eventPublisher, never()).publish("user-1", "task-1", 3);
        verify(heartbeat).close();
    }

    @Test
    void execute_whenPermissionRemainsValid_publishesGeneratedArtifact(@TempDir Path tempDir) throws Exception {
        ExportTask task = lease.task();
        Path target = tempDir.resolve("task-1.xlsx");
        when(authorizer.canExecute(task, Permissions.REPORT_VIEW)).thenReturn(true);
        when(storage.target(lease, "xlsx")).thenReturn(target);
        when(handler.generate(task, target)).thenAnswer(invocation -> {
            Files.writeString(target, "report");
            return new ExportTaskArtifact("report.xlsx", "application/xlsx");
        });
        when(leaseService.markSuccess(any(), any(), any())).thenReturn(true);

        worker.execute(task.getUuid());

        InOrder events = inOrder(eventPublisher);
        events.verify(eventPublisher).publish("user-1", "task-1", 2);
        events.verify(eventPublisher).publish("user-1", "task-1", 3);
        verify(authorizer, times(2)).canExecute(task, Permissions.REPORT_VIEW);
        verify(storage, never()).delete(target);
        verify(heartbeat).close();
    }

    @Test
    void execute_whenLeaseOwnershipIsLost_deletesOnlyItsExecutionArtifact(@TempDir Path tempDir) throws Exception {
        ExportTask task = lease.task();
        Path target = tempDir.resolve("task-1-lease-1.xlsx");
        when(authorizer.canExecute(task, Permissions.REPORT_VIEW)).thenReturn(true);
        when(storage.target(lease, "xlsx")).thenReturn(target);
        when(handler.generate(task, target)).thenAnswer(invocation -> {
            Files.writeString(target, "stale-report");
            return new ExportTaskArtifact("report.xlsx", "application/xlsx");
        });
        when(leaseService.markSuccess(lease, target,
                new ExportTaskArtifact("report.xlsx", "application/xlsx"))).thenReturn(false);

        worker.execute(task.getUuid());

        verify(storage).delete(target);
        verify(eventPublisher, never()).publish("user-1", "task-1", 3);
        verify(heartbeat).close();
    }

    private ExportTask queuedTask() {
        ExportTask task = new ExportTask();
        task.setUuid("task-1");
        task.setTaskType(ReportExportTaskHandler.TASK_TYPE);
        task.setRequesterUuid("user-1");
        task.setTaskStatus(1);
        return task;
    }
}

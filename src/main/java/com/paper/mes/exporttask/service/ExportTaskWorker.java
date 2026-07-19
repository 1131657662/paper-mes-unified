package com.paper.mes.exporttask.service;

import com.paper.mes.exporttask.entity.ExportTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportTaskWorker {
    private static final int STATUS_RUNNING = 2;
    private static final int STATUS_SUCCESS = 3;
    private static final int STATUS_FAILED = 4;
    private static final String GENERIC_FAILURE_MESSAGE = "导出任务执行失败，请重试或联系管理员";
    private static final String ACCESS_REVOKED_MESSAGE = "账号已停用或业务权限已变更，任务已终止";

    private final ExportTaskExecutionLeaseService leaseService;
    private final ExportTaskStorage storage;
    private final ExportTaskHandlerRegistry handlerRegistry;
    private final ExportTaskExecutionAuthorizer executionAuthorizer;
    private final ExportTaskEventPublisher eventPublisher;

    public void execute(String taskUuid) {
        leaseService.claim(taskUuid).ifPresent(this::executeWithHeartbeat);
    }

    private void executeWithHeartbeat(ExportTaskExecutionLease lease) {
        try (ExportTaskHeartbeat ignored = leaseService.startHeartbeat(lease)) {
            executeClaimed(lease);
        }
    }

    private void executeClaimed(ExportTaskExecutionLease lease) {
        ExportTask task = lease.task();
        Path target = null;
        try {
            ExportTaskHandler handler = handlerRegistry.require(task.getTaskType());
            if (!canExecute(task, handler)) {
                failRevokedTask(lease);
                return;
            }
            eventPublisher.publish(task.getRequesterUuid(), task.getUuid(), STATUS_RUNNING);
            target = storage.target(lease, handler.fileExtension());
            ExportTaskArtifact artifact = handler.generate(task, target);
            if (!canExecute(task, handler)) {
                storage.delete(target);
                failRevokedTask(lease);
                return;
            }
            completeTask(lease, target, artifact);
        } catch (Exception exception) {
            storage.delete(target);
            failTask(lease, GENERIC_FAILURE_MESSAGE);
            log.error("Export task failed: {}", task.getUuid(), exception);
        }
    }

    private boolean canExecute(ExportTask task, ExportTaskHandler handler) {
        return executionAuthorizer.canExecute(task, handler.requiredPermission());
    }

    private void completeTask(ExportTaskExecutionLease lease, Path target,
                              ExportTaskArtifact artifact) throws Exception {
        ExportTask task = lease.task();
        if (leaseService.markSuccess(lease, target, artifact)) {
            eventPublisher.publish(task.getRequesterUuid(), task.getUuid(), STATUS_SUCCESS);
        } else {
            storage.delete(target);
        }
    }

    private void failRevokedTask(ExportTaskExecutionLease lease) {
        failTask(lease, ACCESS_REVOKED_MESSAGE);
        log.warn("Export task stopped because requester access changed: {}", lease.task().getUuid());
    }

    private void failTask(ExportTaskExecutionLease lease, String message) {
        ExportTask task = lease.task();
        if (leaseService.markFailure(lease, message)) {
            eventPublisher.publish(task.getRequesterUuid(), task.getUuid(), STATUS_FAILED);
        }
    }
}

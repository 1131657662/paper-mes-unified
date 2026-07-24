package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExportTaskLifecycleService {
    private static final int STATUS_QUEUED = 1;
    private static final int STATUS_FAILED = 4;
    private static final int STATUS_CANCELLED = 5;
    private static final int STATUS_EXPIRED = 6;

    private final ExportTaskMapper taskMapper;
    private final ExportTaskExecutor taskExecutor;
    private final ExportTaskHandlerRegistry handlerRegistry;
    private final PermissionChecker permissionChecker;
    private final ExportTaskEventPublisher eventPublisher;

    public void retry(String taskUuid) {
        ExportTask task = requireAccessible(taskUuid);
        if (!isRetryable(task)) throw new BusinessException("仅失败或已过期的导出任务可以重试");
        if (attemptsExhausted(task)) throw new BusinessException("导出任务已达到最大重试次数");
        if (resetForRetry(taskUuid) == 0) throw new BusinessException("任务状态已变化，请刷新后重试");
        eventPublisher.publish(task.getRequesterUuid(), taskUuid, STATUS_QUEUED);
        taskExecutor.submit(taskUuid);
    }

    public boolean retryScheduled(ExportTask task, CurrentUser recipient) {
        if (!recipient.getUuid().equals(task.getRequesterUuid()) || !isRetryable(task)) return false;
        if (!permissionChecker.hasRolePermission(recipient.getRoleCode(),
                handlerRegistry.require(task.getTaskType()).requiredPermission())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "接收人没有报表权限");
        }
        if (attemptsExhausted(task)) throw new BusinessException("导出任务已达到最大重试次数");
        if (resetForRetry(task.getUuid()) == 0) return false;
        eventPublisher.publish(task.getRequesterUuid(), task.getUuid(), STATUS_QUEUED);
        taskExecutor.submit(task.getUuid());
        return true;
    }

    public void cancel(String taskUuid) {
        CurrentUser user = currentUser();
        ExportTask task = requireAccessible(taskUuid, user);
        LocalDateTime now = LocalDateTime.now();
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<ExportTask>()
                .eq(ExportTask::getUuid, taskUuid).eq(ExportTask::getTaskStatus, STATUS_QUEUED)
                .set(ExportTask::getTaskStatus, STATUS_CANCELLED)
                .set(ExportTask::getCancelledAt, now).set(ExportTask::getCancelledBy, user.getUuid())
                .set(ExportTask::getCompletedAt, now).setSql("version = version + 1"));
        if (updated == 0) throw new BusinessException("任务已开始执行，当前不能取消");
        eventPublisher.publish(task.getRequesterUuid(), taskUuid, STATUS_CANCELLED);
    }

    public void acknowledge(String taskUuid) {
        CurrentUser user = currentUser();
        requireOwned(taskUuid, user);
        taskMapper.update(null, new LambdaUpdateWrapper<ExportTask>()
                .eq(ExportTask::getRequesterUuid, user.getUuid())
                .eq(ExportTask::getUuid, taskUuid).in(
                        ExportTask::getTaskStatus, 3, STATUS_FAILED, STATUS_EXPIRED)
                .set(ExportTask::getAcknowledgedAt, LocalDateTime.now())
                .setSql("version = version + 1"));
    }

    private ExportTask requireAccessible(String uuid) {
        return requireAccessible(uuid, currentUser());
    }

    private ExportTask requireAccessible(String uuid, CurrentUser user) {
        ExportTask task = requireOwned(uuid, user);
        permissionChecker.require(handlerRegistry.require(task.getTaskType()).requiredPermission());
        return task;
    }

    private ExportTask requireOwned(String uuid, CurrentUser user) {
        ExportTask task = taskMapper.selectOne(new LambdaQueryWrapper<ExportTask>()
                .eq(ExportTask::getRequesterUuid, user.getUuid()).eq(ExportTask::getUuid, uuid));
        if (task == null) throw new BusinessException(ResultCode.NOT_FOUND, "导出任务不存在");
        return task;
    }

    private int resetForRetry(String uuid) {
        return taskMapper.update(null, new LambdaUpdateWrapper<ExportTask>()
                .eq(ExportTask::getUuid, uuid).in(ExportTask::getTaskStatus, STATUS_FAILED, STATUS_EXPIRED)
                .set(ExportTask::getTaskStatus, STATUS_QUEUED).set(ExportTask::getProgress, 0)
                .set(ExportTask::getErrorMessage, null).set(ExportTask::getCompletedAt, null)
                .set(ExportTask::getAcknowledgedAt, null).set(ExportTask::getStartedAt, null)
                .set(ExportTask::getHeartbeatAt, null).set(ExportTask::getWorkerId, null)
                .set(ExportTask::getExpiresAt, LocalDateTime.now().plusDays(7))
                .setSql("version = version + 1"));
    }

    private boolean isRetryable(ExportTask task) {
        return Integer.valueOf(STATUS_FAILED).equals(task.getTaskStatus())
                || Integer.valueOf(STATUS_EXPIRED).equals(task.getTaskStatus());
    }

    private boolean attemptsExhausted(ExportTask task) {
        int attempts = task.getAttemptCount() == null ? 0 : task.getAttemptCount();
        int maxAttempts = task.getMaxAttempts() == null ? 3 : task.getMaxAttempts();
        return attempts >= maxAttempts;
    }

    private CurrentUser currentUser() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效");
        }
        return user;
    }
}

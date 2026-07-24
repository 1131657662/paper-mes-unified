package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.exporttask.dto.ExportTaskAcknowledgeDTO;
import com.paper.mes.exporttask.dto.ExportTaskSummaryVO;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExportTaskService {
    private static final int STATUS_QUEUED = 1;
    private static final int STATUS_RUNNING = 2;
    private static final int STATUS_SUCCESS = 3;
    private static final int STATUS_FAILED = 4;
    private static final int STATUS_EXPIRED = 6;

    private final ExportTaskMapper taskMapper;
    private final ExportTaskStorage storage;
    private final PermissionChecker permissionChecker;
    private final ExportTaskHandlerRegistry handlerRegistry;

    public ExportTaskSummaryVO summary() {
        String userUuid = currentUser().getUuid();
        LambdaQueryWrapper<ExportTask> owner = ownerQuery(userUuid);
        long running = taskMapper.selectCount(owner.clone().in(
                ExportTask::getTaskStatus, STATUS_QUEUED, STATUS_RUNNING));
        long unacknowledged = taskMapper.selectCount(owner.clone().in(
                        ExportTask::getTaskStatus, STATUS_SUCCESS, STATUS_FAILED, STATUS_EXPIRED)
                .isNull(ExportTask::getAcknowledgedAt));
        return new ExportTaskSummaryVO(running, unacknowledged);
    }

    public int acknowledge(ExportTaskAcknowledgeDTO filter) {
        String keyword = filter.getKeyword() == null || filter.getKeyword().isBlank()
                ? null : filter.getKeyword().trim();
        return taskMapper.update(null, new LambdaUpdateWrapper<ExportTask>()
                .eq(ExportTask::getRequesterUuid, currentUser().getUuid())
                .in(ExportTask::getTaskStatus, STATUS_SUCCESS, STATUS_FAILED, STATUS_EXPIRED)
                .eq(filter.getTaskStatus() != null, ExportTask::getTaskStatus, filter.getTaskStatus())
                .eq(filter.getModuleCode() != null && !filter.getModuleCode().isBlank(),
                        ExportTask::getModuleCode, filter.getModuleCode())
                .eq(filter.getOperationCode() != null && !filter.getOperationCode().isBlank(),
                        ExportTask::getOperationCode, filter.getOperationCode())
                .and(keyword != null, condition -> condition.like(ExportTask::getTaskName, keyword)
                        .or().like(ExportTask::getFileName, keyword))
                .isNull(ExportTask::getAcknowledgedAt)
                .and(condition -> condition.le(ExportTask::getCompletedAt, filter.getAsOf())
                        .or(legacy -> legacy.isNull(ExportTask::getCompletedAt)
                                .le(ExportTask::getCreateTime, filter.getAsOf())))
                .set(ExportTask::getAcknowledgedAt, LocalDateTime.now()));
    }

    public void download(String taskUuid, HttpServletResponse response) {
        ExportTask task = requireOwned(taskUuid);
        requireTaskPermission(task);
        ensureDownloadable(task);
        Path path = storage.requireFile(task);
        response.setContentType(task.getContentType() == null
                ? "application/octet-stream" : task.getContentType());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(task.getFileName(), StandardCharsets.UTF_8).build().toString());
        try {
            Files.copy(path, response.getOutputStream());
            markDownloaded(taskUuid);
        } catch (IOException exception) {
            throw new BusinessException("读取导出文件失败");
        }
    }

    private void ensureDownloadable(ExportTask task) {
        if (!Integer.valueOf(STATUS_SUCCESS).equals(task.getTaskStatus())) {
            String message = Integer.valueOf(STATUS_EXPIRED).equals(task.getTaskStatus())
                    ? "导出文件已过期，请重新发起" : "导出任务尚未完成";
            throw new BusinessException(message);
        }
        if (task.getExpiresAt() != null && task.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("导出文件已过期，请重新发起");
        }
    }

    private ExportTask requireOwned(String uuid) {
        ExportTask task = taskMapper.selectOne(ownerQuery(currentUser().getUuid())
                .eq(ExportTask::getUuid, uuid));
        if (task == null) throw new BusinessException(ResultCode.NOT_FOUND, "导出任务不存在");
        return task;
    }

    private LambdaQueryWrapper<ExportTask> ownerQuery(String userUuid) {
        return new LambdaQueryWrapper<ExportTask>().eq(ExportTask::getRequesterUuid, userUuid);
    }

    private void requireTaskPermission(ExportTask task) {
        permissionChecker.require(handlerRegistry.require(task.getTaskType()).requiredPermission());
    }

    private void markDownloaded(String uuid) {
        LocalDateTime now = LocalDateTime.now();
        taskMapper.update(null, new LambdaUpdateWrapper<ExportTask>()
                .eq(ExportTask::getUuid, uuid).eq(ExportTask::getTaskStatus, STATUS_SUCCESS)
                .set(ExportTask::getDownloadedAt, now).set(ExportTask::getAcknowledgedAt, now)
                .setSql("download_count = download_count + 1, version = version + 1"));
    }

    private CurrentUser currentUser() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效");
        }
        return user;
    }
}

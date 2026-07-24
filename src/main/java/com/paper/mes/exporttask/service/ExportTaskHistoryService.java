package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.exporttask.dto.ExportTaskHistoryVO;
import com.paper.mes.exporttask.dto.ExportTaskHistoryQuery;
import com.paper.mes.exporttask.dto.ExportTaskVO;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExportTaskHistoryService {
    private static final int STATUS_SUCCESS = 3;
    private static final int STATUS_FAILED = 4;
    private static final int STATUS_EXPIRED = 6;

    private final ExportTaskMapper taskMapper;
    private final ExportTaskHandlerRegistry handlerRegistry;
    private final PermissionChecker permissionChecker;

    public ExportTaskHistoryVO page(ExportTaskHistoryQuery query) {
        LocalDateTime asOf = LocalDateTime.now();
        String keyword = normalizeKeyword(query.getKeyword());
        Page<ExportTask> taskPage = taskMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()),
                new LambdaQueryWrapper<ExportTask>()
                        .eq(ExportTask::getRequesterUuid, currentUserUuid())
                        .le(ExportTask::getCreateTime, asOf)
                        .eq(query.getTaskStatus() != null, ExportTask::getTaskStatus, query.getTaskStatus())
                        .eq(query.getModuleCode() != null && !query.getModuleCode().isBlank(),
                                ExportTask::getModuleCode, query.getModuleCode())
                        .eq(query.getOperationCode() != null && !query.getOperationCode().isBlank(),
                                ExportTask::getOperationCode, query.getOperationCode())
                        .and(keyword != null, condition -> condition
                                .like(ExportTask::getTaskName, keyword)
                                .or().like(ExportTask::getFileName, keyword))
                        .and(Boolean.TRUE.equals(query.getAttentionOnly()), condition -> condition
                                .in(ExportTask::getTaskStatus, STATUS_SUCCESS, STATUS_FAILED, STATUS_EXPIRED)
                                .isNull(ExportTask::getAcknowledgedAt))
                        .orderByDesc(ExportTask::getCreateTime)
                        .orderByDesc(ExportTask::getUuid));
        return new ExportTaskHistoryVO(
                taskPage.getRecords().stream().map(this::toView).toList(),
                taskPage.getTotal(), taskPage.getCurrent(), taskPage.getSize(), asOf);
    }

    private ExportTaskVO toView(ExportTask task) {
        boolean accessible = handlerRegistry.find(task.getTaskType())
                .map(ExportTaskHandler::requiredPermission)
                .map(permissionChecker::has)
                .orElse(false);
        return ExportTaskVO.from(task, accessible);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return keyword.trim();
    }

    private String currentUserUuid() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效");
        }
        return user.getUuid();
    }
}

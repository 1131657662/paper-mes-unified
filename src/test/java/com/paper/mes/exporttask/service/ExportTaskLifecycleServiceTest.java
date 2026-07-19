package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportTaskLifecycleServiceTest {
    private ExportTaskMapper taskMapper;
    private ExportTaskExecutor executor;
    private PermissionChecker permissionChecker;
    private ExportTaskEventPublisher eventPublisher;
    private ExportTaskLifecycleService service;

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExportTask.class);
    }

    @BeforeEach
    void setUp() {
        taskMapper = mock(ExportTaskMapper.class);
        executor = mock(ExportTaskExecutor.class);
        permissionChecker = mock(PermissionChecker.class);
        eventPublisher = mock(ExportTaskEventPublisher.class);
        ExportTaskHandler handler = mock(ExportTaskHandler.class);
        when(handler.taskType()).thenReturn(SettleDetailExportTaskHandler.TASK_TYPE);
        when(handler.requiredPermission()).thenReturn(Permissions.SETTLE_VIEW);
        service = new ExportTaskLifecycleService(taskMapper, executor,
                new ExportTaskHandlerRegistry(List.of(handler)), permissionChecker, eventPublisher);
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1").username("operator").build());
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void retry_failedTask_requeuesAndSubmits() {
        ExportTask task = task(4);
        task.setAttemptCount(1);
        task.setMaxAttempts(3);
        when(taskMapper.selectOne(any())).thenReturn(task);
        when(taskMapper.update(isNull(), any())).thenReturn(1);

        service.retry(task.getUuid());

        verify(permissionChecker).require(Permissions.SETTLE_VIEW);
        verify(eventPublisher).publish("user-1", task.getUuid(), 1);
        verify(executor).submit(task.getUuid());
    }

    @Test
    void retry_exhaustedTask_rejectsWithoutSubmitting() {
        ExportTask task = task(4);
        task.setAttemptCount(3);
        task.setMaxAttempts(3);
        when(taskMapper.selectOne(any())).thenReturn(task);

        assertThatThrownBy(() -> service.retry(task.getUuid()))
                .hasMessageContaining("最大重试次数");
    }

    @Test
    void cancel_queuedTask_updatesState() {
        ExportTask task = task(1);
        when(taskMapper.selectOne(any())).thenReturn(task);
        when(taskMapper.update(isNull(), any())).thenReturn(1);

        service.cancel(task.getUuid());

        verify(permissionChecker).require(Permissions.SETTLE_VIEW);
        verify(eventPublisher).publish("user-1", task.getUuid(), 5);
    }

    @Test
    void acknowledge_ownedTask_doesNotRequireRevokedSourcePermission() {
        ExportTask task = task(4);
        when(taskMapper.selectOne(any())).thenReturn(task);
        when(taskMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenAnswer(invocation -> {
            LambdaUpdateWrapper<ExportTask> wrapper = invocation.getArgument(1);
            assertThat(wrapper.getCustomSqlSegment()).contains("requester_uuid", "uuid", "task_status IN");
            assertThat(wrapper.getParamNameValuePairs()).containsValues("user-1", task.getUuid());
            return 1;
        });

        service.acknowledge(task.getUuid());

        verifyNoInteractions(permissionChecker);
    }

    private ExportTask task(int status) {
        ExportTask task = new ExportTask();
        task.setUuid("task-1");
        task.setTaskType(SettleDetailExportTaskHandler.TASK_TYPE);
        task.setRequesterUuid("user-1");
        task.setTaskStatus(status);
        return task;
    }
}

package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.exporttask.dto.ExportTaskHistoryQuery;
import com.paper.mes.exporttask.dto.ExportTaskHistoryVO;
import com.paper.mes.exporttask.dto.ExportTaskVO;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportTaskHistoryServiceTest {
    private ExportTaskMapper taskMapper;
    private PermissionChecker permissionChecker;
    private ExportTaskHistoryService service;

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExportTask.class);
    }

    @BeforeEach
    void setUp() {
        taskMapper = mock(ExportTaskMapper.class);
        permissionChecker = mock(PermissionChecker.class);
        ExportTaskHandler handler = mock(ExportTaskHandler.class);
        when(handler.taskType()).thenReturn(ReportExportTaskHandler.TASK_TYPE);
        when(handler.requiredPermission()).thenReturn(Permissions.REPORT_VIEW);
        when(permissionChecker.has(Permissions.REPORT_VIEW)).thenReturn(true);
        service = new ExportTaskHistoryService(taskMapper,
                new ExportTaskHandlerRegistry(List.of(handler)), permissionChecker);
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1").username("operator").build());
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void page_returnsRequestedPageScopedToCurrentUser() {
        when(taskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            Page<ExportTask> page = invocation.getArgument(0);
            LambdaQueryWrapper<ExportTask> wrapper = invocation.getArgument(1);
            assertThat(wrapper.getCustomSqlSegment()).contains(
                    "requester_uuid", "create_time", "task_name", "file_name", "ORDER BY");
            assertThat(wrapper.getParamNameValuePairs()).containsValues(
                    "user-1", 3, "settle", "detail-export", "%JS2026%");
            page.setRecords(List.of(task("task-2"), task("task-1")));
            page.setTotal(42);
            return page;
        });
        ExportTaskHistoryQuery query = new ExportTaskHistoryQuery();
        query.setCurrent(2);
        query.setSize(10);
        query.setTaskStatus(3);
        query.setModuleCode("settle");
        query.setOperationCode("detail-export");
        query.setKeyword("  JS2026  ");

        ExportTaskHistoryVO result = service.page(query);

        assertThat(result.current()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.total()).isEqualTo(42);
        assertThat(result.asOf()).isNotNull();
        assertThat(result.records()).extracting(ExportTaskVO::uuid).containsExactly("task-2", "task-1");
        assertThat(result.records()).allMatch(ExportTaskVO::resourceAccessible);
    }

    @Test
    void page_attentionOnly_filtersUnacknowledgedTerminalTasks() {
        when(taskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            Page<ExportTask> page = invocation.getArgument(0);
            LambdaQueryWrapper<ExportTask> wrapper = invocation.getArgument(1);
            assertThat(wrapper.getCustomSqlSegment())
                    .contains("task_status IN")
                    .contains("acknowledged_at IS NULL");
            return page;
        });
        ExportTaskHistoryQuery query = new ExportTaskHistoryQuery();
        query.setAttentionOnly(true);

        service.page(query);
    }

    @Test
    void page_whenSourcePermissionRevoked_keepsRecordWithoutResourceAccess() {
        when(permissionChecker.has(Permissions.REPORT_VIEW)).thenReturn(false);
        when(taskMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
            Page<ExportTask> page = invocation.getArgument(0);
            page.setRecords(List.of(task("task-1")));
            return page;
        });

        ExportTaskHistoryVO result = service.page(new ExportTaskHistoryQuery());

        assertThat(result.records()).singleElement()
                .extracting(ExportTaskVO::resourceAccessible).isEqualTo(false);
    }

    private ExportTask task(String uuid) {
        ExportTask task = new ExportTask();
        task.setUuid(uuid);
        task.setTaskType(ReportExportTaskHandler.TASK_TYPE);
        task.setTaskName("报表导出");
        task.setTaskStatus(3);
        task.setProgress(100);
        return task;
    }
}

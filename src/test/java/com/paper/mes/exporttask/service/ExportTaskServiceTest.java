package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.exporttask.dto.ExportTaskAcknowledgeDTO;
import com.paper.mes.exporttask.entity.ExportTask;
import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportTaskServiceTest {
    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExportTask.class);
    }

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void acknowledge_appliesOwnerAndVisibleFilters() {
        ExportTaskMapper mapper = mock(ExportTaskMapper.class);
        ExportTaskService service = service(mapper);
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-1").build());
        when(mapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenAnswer(invocation -> {
            LambdaUpdateWrapper<ExportTask> wrapper = invocation.getArgument(1);
            assertThat(wrapper.getCustomSqlSegment()).contains(
                    "requester_uuid", "task_status IN", "module_code", "task_name", "file_name",
                    "acknowledged_at IS NULL", "completed_at", "create_time");
            assertThat(wrapper.getParamNameValuePairs()).containsValues(
                    "user-1", 4, "settle", "%JS2026%", LocalDateTime.of(2026, 7, 19, 15, 30));
            return 2;
        });
        ExportTaskAcknowledgeDTO filter = new ExportTaskAcknowledgeDTO();
        filter.setTaskStatus(4);
        filter.setModuleCode("settle");
        filter.setKeyword(" JS2026 ");
        filter.setAsOf(LocalDateTime.of(2026, 7, 19, 15, 30));

        int result = service.acknowledge(filter);

        assertThat(result).isEqualTo(2);
    }

    private ExportTaskService service(ExportTaskMapper mapper) {
        return new ExportTaskService(mapper, mock(ExportTaskStorage.class), mock(PermissionChecker.class),
                new ExportTaskHandlerRegistry(List.of()));
    }
}

package com.paper.mes.exporttask.service;

import com.paper.mes.exporttask.mapper.ExportTaskMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportTaskExpirationServiceTest {

    @Test
    void expire_whenTaskTransitionsToExpired_deletesOwnedSnapshot() {
        ExportTaskMapper taskMapper = mock(ExportTaskMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(taskMapper.update(isNull(), any())).thenReturn(1);
        ExportTaskExpirationService service = new ExportTaskExpirationService(taskMapper, jdbcTemplate);

        assertThat(service.expire("task-1")).isEqualTo(1);

        verify(jdbcTemplate).update("DELETE FROM sys_export_snapshot WHERE task_uuid = ?", "task-1");
    }

    @Test
    void expire_whenTaskStateChanged_preservesSnapshot() {
        ExportTaskMapper taskMapper = mock(ExportTaskMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(taskMapper.update(isNull(), any())).thenReturn(0);
        ExportTaskExpirationService service = new ExportTaskExpirationService(taskMapper, jdbcTemplate);

        assertThat(service.expire("task-1")).isZero();

        verify(jdbcTemplate, never()).update(any(String.class), any(Object.class));
    }
}

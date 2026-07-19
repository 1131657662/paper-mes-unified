package com.paper.mes.exporttask.service;

import com.paper.mes.exporttask.dto.ExportTaskOperationsIssueVO;
import com.paper.mes.exporttask.config.ExportTaskRuntimeProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportTaskOperationsServiceTest {
    @Test
    void issues_returnsStaleAndRecentFailedTasksSeparately() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        var stale = issue("stale-1", 2);
        var failed = issue("failed-1", 4);
        when(jdbcTemplate.query(contains("task_status = 2"), any(RowMapper.class), any(), any()))
                .thenReturn(List.of(stale));
        when(jdbcTemplate.query(contains("task_status = 4"), any(RowMapper.class), any()))
                .thenReturn(List.of(failed));
        var publisher = new ExportTaskEventPublisher(new SimpleMeterRegistry());
        var service = new ExportTaskOperationsService(jdbcTemplate, publisher,
                mock(ExportTaskExecutor.class), runtimeProperties(), mock(ExportTaskStorage.class));

        var result = service.issues();

        assertThat(result.staleTasks()).containsExactly(stale);
        assertThat(result.failedTasks()).containsExactly(failed);
        assertThat(result.asOf()).isNotNull();
    }

    @Test
    void overview_includesCurrentExecutorCapacitySnapshot() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ResultSet resultSet = operationsResultSet();
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), any(Timestamp.class)))
                .thenAnswer(invocation -> invocation.<RowMapper<?>>getArgument(1).mapRow(resultSet, 0));
        ExportTaskExecutor executor = mock(ExportTaskExecutor.class);
        when(executor.snapshot()).thenReturn(new ExportTaskExecutorSnapshot(2, 1, 7, 50, 3, 12));
        ExportTaskStorage storage = mock(ExportTaskStorage.class);
        when(storage.health(0, 5)).thenReturn(new ExportTaskStorageHealth(
                ExportTaskStorageHealth.READY, true, true,
                8_000_000_000L, 16_000_000_000L, 50, LocalDateTime.now()));
        var service = new ExportTaskOperationsService(jdbcTemplate,
                new ExportTaskEventPublisher(new SimpleMeterRegistry()), executor, runtimeProperties(),
                storage);

        var result = service.overview();

        assertThat(result.workerCount()).isEqualTo(2);
        assertThat(result.activeWorkerCount()).isEqualTo(1);
        assertThat(result.queuedInMemoryCount()).isEqualTo(7);
        assertThat(result.queueCapacity()).isEqualTo(50);
        assertThat(result.rejectedSubmissionCount()).isEqualTo(3);
        assertThat(result.completedExecutionCount()).isEqualTo(12);
        assertThat(result.storageStatus()).isEqualTo(ExportTaskStorageHealth.READY);
        assertThat(result.storageAvailable()).isTrue();
        assertThat(result.storageFreePercent()).isEqualTo(50);
    }

    private ResultSet operationsResultSet() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getLong(anyString())).thenReturn(0L);
        when(resultSet.getDouble(anyString())).thenReturn(0D);
        when(resultSet.getTimestamp("oldest_queued_at")).thenReturn(null);
        return resultSet;
    }

    private ExportTaskOperationsIssueVO issue(String uuid, int status) {
        return new ExportTaskOperationsIssueVO(uuid, "任务", "操作员", "report", status,
                null, LocalDateTime.now(), null, null);
    }

    private ExportTaskRuntimeProperties runtimeProperties() {
        return new ExportTaskRuntimeProperties();
    }
}

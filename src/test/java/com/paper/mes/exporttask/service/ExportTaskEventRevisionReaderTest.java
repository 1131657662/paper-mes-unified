package com.paper.mes.exporttask.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportTaskEventRevisionReaderTest {
    @Test
    void read_returnsStableEmptyRevisionForUsersWithoutTasks() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        ExportTaskEventRevisionReader reader = new ExportTaskEventRevisionReader(jdbcTemplate);

        Map<String, String> revisions = reader.read(Set.of("user-1", "user-2"));

        assertThat(revisions).containsEntry("user-1", "0:0").containsEntry("user-2", "0:0");
    }

    @Test
    void read_combinesTaskCountAndVersionSumIntoRevision() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(
                Map.of("requester_uuid", "user-1", "task_count", 2L, "version_sum", 7L)));
        ExportTaskEventRevisionReader reader = new ExportTaskEventRevisionReader(jdbcTemplate);

        Map<String, String> revisions = reader.read(Set.of("user-1"));

        assertThat(revisions).containsEntry("user-1", "2:7");
    }
}

package com.paper.mes.report.materialization;

import com.paper.mes.report.materialization.service.ReportMaterializationJobService;
import com.paper.mes.report.materialization.service.ReportMaterializationLease;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportMaterializationJobServiceTest {
    @Test
    void claim_availableJob_returnsIncrementedFencingLease() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ReportMaterializationJobService service = new ReportMaterializationJobService(jdbc);
        when(jdbc.update(anyString(), any(), any(), any(), any(), any())).thenReturn(1);
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenReturn(List.of(new ReportMaterializationLease(
                        "job-1", "worker-1", 7, LocalDateTime.now().plusMinutes(5))));

        var lease = service.claim("job-1", "worker-1", Duration.ofMinutes(5));

        assertThat(lease).isPresent();
        assertThat(lease.orElseThrow().fencingToken()).isEqualTo(7);
    }

    @Test
    void heartbeat_staleFencingToken_returnsFalse() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        ReportMaterializationJobService service = new ReportMaterializationJobService(jdbc);
        when(jdbc.update(anyString(), any(), any(), any(), any())).thenReturn(0);

        boolean result = service.heartbeat(
                new ReportMaterializationLease("job-1", "worker-1", 3, LocalDateTime.now()),
                Duration.ofMinutes(5));

        assertThat(result).isFalse();
        ArgumentCaptor<Object> token = ArgumentCaptor.forClass(Object.class);
        verify(jdbc).update(anyString(), any(), any(), any(), token.capture());
        assertThat(token.getValue()).isEqualTo(3L);
    }
}

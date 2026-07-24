package com.paper.mes.report.alert;

import com.paper.mes.report.alert.service.ReportAlertEventRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportAlertEventRecorderCleanupTest {
    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void cleanupCommands_returnResolvedRowCounts() {
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(3, 2, 1, 4);
        ReportAlertEventRecorder recorder = new ReportAlertEventRecorder(jdbcTemplate);

        assertThat(recorder.resolveExpired(LocalDate.of(2026, 7, 1))).isEqualTo(3);
        assertThat(recorder.resolveInactiveRules()).isEqualTo(2);
        assertThat(recorder.resolveSupersededRelease("release-1")).isEqualTo(1);
        assertThat(recorder.resolveOutdatedScopes(
                "release-1", LocalDate.of(2026, 7, 1))).isEqualTo(4);
    }
}

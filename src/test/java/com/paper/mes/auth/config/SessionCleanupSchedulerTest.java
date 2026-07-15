package com.paper.mes.auth.config;

import com.paper.mes.auth.mapper.SysUserSessionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SessionCleanupSchedulerTest {

    @Test
    void cleanupExpiredSessions_usesConfiguredRetentionPeriod() {
        SysUserSessionMapper mapper = mock(SysUserSessionMapper.class);
        AuthProperties properties = new AuthProperties();
        properties.setSessionRetentionDays(7);
        SessionCleanupScheduler scheduler = new SessionCleanupScheduler(mapper, properties);
        LocalDateTime expected = LocalDateTime.now().minusDays(7);

        scheduler.cleanupExpiredSessions();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).deleteExpiredBefore(cutoff.capture());
        assertTrue(Math.abs(java.time.Duration.between(expected, cutoff.getValue()).toSeconds()) <= 1);
    }
}

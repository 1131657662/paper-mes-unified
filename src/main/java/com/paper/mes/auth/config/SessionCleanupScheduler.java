package com.paper.mes.auth.config;

import com.paper.mes.auth.mapper.SysUserSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupScheduler {

    private final SysUserSessionMapper sessionMapper;
    private final AuthProperties authProperties;

    @Scheduled(cron = "${app.auth.session-cleanup-cron:0 30 3 * * *}")
    public void cleanupExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusDays(authProperties.getSessionRetentionDays());
        try {
            int deleted = sessionMapper.deleteExpiredBefore(cutoff);
            if (deleted > 0) {
                log.info("Deleted {} expired login sessions", deleted);
            }
        } catch (RuntimeException exception) {
            log.error("Scheduled login session cleanup failed", exception);
        }
    }
}

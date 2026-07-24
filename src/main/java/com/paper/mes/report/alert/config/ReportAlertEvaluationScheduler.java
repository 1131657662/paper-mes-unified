package com.paper.mes.report.alert.config;

import com.paper.mes.report.alert.service.ReportAlertEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportAlertEvaluationScheduler {
    private static final String LOCK_NAME = "paper-mes:report-alert-evaluation";
    private static final ZoneId STORAGE_ZONE = ZoneId.of("Asia/Shanghai");
    private final JdbcTemplate jdbcTemplate;
    private final ReportAlertEvaluationService evaluationService;

    @Scheduled(initialDelayString = "${app.report.alert-initial-delay-ms:60000}",
            fixedDelayString = "${app.report.alert-check-delay-ms:900000}")
    public void evaluate() {
        try {
            jdbcTemplate.execute((ConnectionCallback<Void>) connection -> runWithLock(connection));
        } catch (RuntimeException exception) {
            log.error("Scheduled report alert evaluation failed", exception);
        }
    }

    private Void runWithLock(Connection connection) {
        if (!acquire(connection)) return null;
        try {
            evaluationService.evaluate(LocalDate.now(STORAGE_ZONE));
        } finally {
            release(connection);
        }
        return null;
    }

    private boolean acquire(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, 0)")) {
            statement.setString(1, LOCK_NAME);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) == 1;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to acquire report alert scheduler lock", exception);
        }
    }

    private void release(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, LOCK_NAME);
            statement.executeQuery();
        } catch (Exception exception) {
            log.warn("Unable to release report alert scheduler lock", exception);
        }
    }
}

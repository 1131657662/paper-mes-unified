package com.paper.mes.report.subscription.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.report.subscription.entity.ReportSubscription;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionMapper;
import com.paper.mes.report.subscription.service.ReportSubscriptionDispatchService;
import com.paper.mes.report.subscription.service.ReportSubscriptionSchedulePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportSubscriptionScheduler {
    private static final String LOCK_NAME = "paper-mes:report-subscription-scheduler";
    private static final int BATCH_SIZE = 50;
    private final JdbcTemplate jdbcTemplate;
    private final ReportSubscriptionMapper subscriptionMapper;
    private final ReportSubscriptionDispatchService dispatchService;

    @Scheduled(fixedDelayString = "${app.report.subscription-check-delay-ms:60000}")
    public void dispatchDueSubscriptions() {
        try {
            jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
                if (!acquire(connection)) return null;
                try {
                    dispatchBatch(LocalDateTime.now(ReportSubscriptionSchedulePolicy.STORAGE_ZONE));
                } finally {
                    release(connection);
                }
                return null;
            });
        } catch (RuntimeException exception) {
            log.error("Scheduled report subscription dispatch failed", exception);
        }
    }

    private void dispatchBatch(LocalDateTime now) {
        List<ReportSubscription> due = subscriptionMapper.selectList(
                new LambdaQueryWrapper<ReportSubscription>()
                        .eq(ReportSubscription::getIsEnabled, 1)
                        .le(ReportSubscription::getNextRunAt, now)
                        .orderByAsc(ReportSubscription::getNextRunAt, ReportSubscription::getUuid)
                        .last("LIMIT " + BATCH_SIZE));
        for (ReportSubscription subscription : due) {
            try {
                dispatchService.dispatch(subscription, now);
            } catch (RuntimeException exception) {
                log.error("Report subscription dispatch failed: {}", subscription.getUuid(), exception);
            }
        }
    }

    private boolean acquire(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, 0)")) {
            statement.setString(1, LOCK_NAME);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) == 1;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to acquire report subscription scheduler lock", exception);
        }
    }

    private void release(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, LOCK_NAME);
            statement.executeQuery();
        } catch (Exception exception) {
            log.warn("Unable to release report subscription scheduler lock", exception);
        }
    }
}

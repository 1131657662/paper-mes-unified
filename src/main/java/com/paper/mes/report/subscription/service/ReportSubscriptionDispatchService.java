package com.paper.mes.report.subscription.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.subscription.entity.ReportSubscription;
import com.paper.mes.report.subscription.entity.ReportSubscriptionRun;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportSubscriptionDispatchService {
    private final ReportMetricReleaseResolver releaseResolver;
    private final ReportSubscriptionCodec codec;
    private final ReportSubscriptionRunService runService;
    private final ReportSubscriptionRecipientDispatcher recipientDispatcher;
    private final ReportSubscriptionMapper subscriptionMapper;

    public void dispatch(ReportSubscription subscription, LocalDateTime now) {
        LocalDateTime scheduledFor = subscription.getNextRunAt();
        ReportSubscriptionRun run = dispatchAt(subscription, scheduledFor);
        advance(subscription, scheduledFor, now, run.getErrorMessage());
    }

    public ReportSubscriptionRun dispatchNow(ReportSubscription subscription, LocalDateTime now) {
        return dispatchAt(subscription, now.truncatedTo(ChronoUnit.MINUTES));
    }

    public ReportSubscriptionRun retry(ReportSubscription subscription, LocalDateTime scheduledFor) {
        return dispatchAt(subscription, scheduledFor);
    }

    private ReportSubscriptionRun dispatchAt(ReportSubscription subscription, LocalDateTime scheduledFor) {
        var start = runService.start(subscription.getUuid(), scheduledFor);
        ReportSubscriptionRun run = start.run();
        if (!start.claimed()) return run;
        try {
            dispatchClaimed(subscription, scheduledFor, run);
        } catch (RuntimeException exception) {
            log.error("Report subscription run failed: {}", subscription.getUuid(), exception);
            runService.fail(run, exception);
        }
        return run;
    }

    private void dispatchClaimed(ReportSubscription subscription, LocalDateTime scheduledFor,
                                 ReportSubscriptionRun run) {
        String releaseUuid = release(run, subscription);
        ReportQuery query = resolveQuery(subscription, scheduledFor, releaseUuid);
        var result = recipientDispatcher.dispatch(subscription, scheduledFor, query);
        runService.complete(run, result);
    }

    private String release(ReportSubscriptionRun run, ReportSubscription subscription) {
        if (run.getMetricReleaseUuid() != null && !run.getMetricReleaseUuid().isBlank()) {
            return run.getMetricReleaseUuid();
        }
        String releaseUuid = releaseResolver.resolve(subscription);
        runService.bindRelease(run, releaseUuid);
        return releaseUuid;
    }

    private ReportQuery resolveQuery(ReportSubscription subscription, LocalDateTime scheduledFor,
                                     String releaseUuid) {
        LocalDate localDate = scheduledFor.atZone(ReportSubscriptionSchedulePolicy.STORAGE_ZONE)
                .withZoneSameInstant(ZoneId.of(subscription.getTimezone())).toLocalDate();
        ReportQuery query = ReportSubscriptionPeriodPolicy.resolve(
                codec.read(subscription.getReportQuery()), subscription.getPeriodPolicy(), localDate);
        query.setMetricReleaseUuid(releaseUuid);
        return query;
    }

    private void advance(ReportSubscription subscription, LocalDateTime scheduledFor,
                         LocalDateTime now, String errorMessage) {
        LocalDateTime nextRun = ReportSubscriptionSchedulePolicy.nextRun(subscription, now);
        int rows = subscriptionMapper.update(null, updateGuard(subscription, scheduledFor)
                .set(ReportSubscription::getLastScheduledAt, scheduledFor)
                .set(ReportSubscription::getNextRunAt, nextRun)
                .set(ReportSubscription::getLastErrorMessage, errorMessage)
                .setSql("version = version + 1"));
        ConcurrencyGuard.requireRowUpdated(rows);
    }

    private LambdaUpdateWrapper<ReportSubscription> updateGuard(ReportSubscription subscription,
                                                                 LocalDateTime scheduledFor) {
        return new LambdaUpdateWrapper<ReportSubscription>()
                .eq(ReportSubscription::getUuid, subscription.getUuid())
                .eq(ReportSubscription::getNextRunAt, scheduledFor)
                .eq(ReportSubscription::getIsEnabled, 1);
    }
}

package com.paper.mes.report.subscription.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.report.subscription.entity.ReportSubscriptionRun;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReportSubscriptionRunService {
    private static final int RUNNING = 1;
    private static final int SUCCESS = 2;
    private static final int PARTIAL = 3;
    private static final int FAILED = 4;
    private final ReportSubscriptionRunMapper runMapper;

    public StartResult start(String subscriptionUuid, LocalDateTime scheduledFor) {
        ReportSubscriptionRun run = find(subscriptionUuid, scheduledFor);
        if (run != null) return claimExisting(run);
        run = new ReportSubscriptionRun();
        run.setSubscriptionUuid(subscriptionUuid);
        run.setScheduledFor(scheduledFor);
        reset(run);
        try {
            ConcurrencyGuard.requireRowUpdated(runMapper.insert(run));
            return new StartResult(run, true);
        } catch (DuplicateKeyException exception) {
            return new StartResult(find(subscriptionUuid, scheduledFor), false);
        }
    }

    public void bindRelease(ReportSubscriptionRun run, String releaseUuid) {
        run.setMetricReleaseUuid(releaseUuid);
        int rows = runMapper.update(null, new LambdaUpdateWrapper<ReportSubscriptionRun>()
                .eq(ReportSubscriptionRun::getUuid, run.getUuid())
                .eq(ReportSubscriptionRun::getRunStatus, RUNNING)
                .set(ReportSubscriptionRun::getMetricReleaseUuid, releaseUuid));
        ConcurrencyGuard.requireRowUpdated(rows);
    }

    public void complete(ReportSubscriptionRun run,
                         ReportSubscriptionRecipientDispatcher.DispatchResult result) {
        run.setPlannedCount(result.planned());
        run.setDispatchedCount(result.dispatched());
        run.setFailedCount(result.failed());
        run.setRunStatus(status(result));
        run.setErrorMessage(result.errorMessage());
        run.setCompletedAt(LocalDateTime.now(ReportSubscriptionSchedulePolicy.STORAGE_ZONE));
        ConcurrencyGuard.requireRowUpdated(runMapper.updateById(run));
    }

    public void fail(ReportSubscriptionRun run, RuntimeException exception) {
        run.setRunStatus(FAILED);
        run.setErrorMessage(failureMessage(exception));
        run.setCompletedAt(LocalDateTime.now(ReportSubscriptionSchedulePolicy.STORAGE_ZONE));
        ConcurrencyGuard.requireRowUpdated(runMapper.updateById(run));
    }

    private ReportSubscriptionRun find(String subscriptionUuid, LocalDateTime scheduledFor) {
        return runMapper.selectOne(new LambdaQueryWrapper<ReportSubscriptionRun>()
                .eq(ReportSubscriptionRun::getSubscriptionUuid, subscriptionUuid)
                .eq(ReportSubscriptionRun::getScheduledFor, scheduledFor));
    }

    private StartResult claimExisting(ReportSubscriptionRun run) {
        if (run.getRunStatus() != FAILED && run.getRunStatus() != PARTIAL) {
            return new StartResult(run, false);
        }
        int rows = runMapper.update(null, retryableRunClaim(run));
        if (rows == 0) return new StartResult(find(run.getSubscriptionUuid(), run.getScheduledFor()), false);
        reset(run);
        return new StartResult(run, true);
    }

    private LambdaUpdateWrapper<ReportSubscriptionRun> retryableRunClaim(ReportSubscriptionRun run) {
        return new LambdaUpdateWrapper<ReportSubscriptionRun>()
                .eq(ReportSubscriptionRun::getUuid, run.getUuid())
                .in(ReportSubscriptionRun::getRunStatus, PARTIAL, FAILED)
                .set(ReportSubscriptionRun::getRunStatus, RUNNING)
                .set(ReportSubscriptionRun::getPlannedCount, 0)
                .set(ReportSubscriptionRun::getDispatchedCount, 0)
                .set(ReportSubscriptionRun::getFailedCount, 0)
                .set(ReportSubscriptionRun::getErrorMessage, null)
                .set(ReportSubscriptionRun::getCompletedAt, null);
    }

    private void reset(ReportSubscriptionRun run) {
        run.setRunStatus(RUNNING);
        run.setPlannedCount(0);
        run.setDispatchedCount(0);
        run.setFailedCount(0);
        run.setErrorMessage(null);
        run.setCompletedAt(null);
    }

    private int status(ReportSubscriptionRecipientDispatcher.DispatchResult result) {
        if (result.failed() == 0) return SUCCESS;
        return result.dispatched() > 0 ? PARTIAL : FAILED;
    }

    private String failureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return "报表订阅执行失败";
        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    public record StartResult(ReportSubscriptionRun run, boolean claimed) {
    }
}

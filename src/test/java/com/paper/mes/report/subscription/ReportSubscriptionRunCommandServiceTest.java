package com.paper.mes.report.subscription;

import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.report.subscription.entity.ReportSubscription;
import com.paper.mes.report.subscription.entity.ReportSubscriptionRun;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionMapper;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionRunMapper;
import com.paper.mes.report.subscription.service.ReportSubscriptionAccessPolicy;
import com.paper.mes.report.subscription.service.ReportSubscriptionDispatchService;
import com.paper.mes.report.subscription.service.ReportSubscriptionRunCommandService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportSubscriptionRunCommandServiceTest {
    @Test
    void runNow_recordsRunNowAction() {
        Fixture fixture = fixture();
        when(fixture.dispatch().dispatchNow(any(), any())).thenReturn(run("run-now", 1));

        String result = fixture.service().runNow("subscription-1");

        assertEquals("run-now", result);
        verify(fixture.logs()).record(OperationLogService.BIZ_TYPE_REPORT, "subscription-1", "日报",
                OperationLogService.ACTION_REPORT_SUBSCRIPTION_RUN_NOW, null, "手动试跑");
    }

    @Test
    void retry_recordsRetryAction() {
        Fixture fixture = fixture();
        ReportSubscriptionRun failed = run("failed-run", 4);
        when(fixture.runMapper().selectById("failed-run")).thenReturn(failed);
        when(fixture.dispatch().retry(any(), any())).thenReturn(run("retried-run", 1));

        String result = fixture.service().retry("subscription-1", "failed-run");

        assertEquals("retried-run", result);
        verify(fixture.logs()).record(eq(OperationLogService.BIZ_TYPE_REPORT), eq("subscription-1"), eq("日报"),
                eq(OperationLogService.ACTION_REPORT_SUBSCRIPTION_RETRY), isNull(), startsWith("重试订阅运行"));
    }

    private Fixture fixture() {
        ReportSubscriptionMapper subscriptions = mock(ReportSubscriptionMapper.class);
        ReportSubscriptionRunMapper runs = mock(ReportSubscriptionRunMapper.class);
        ReportSubscriptionAccessPolicy access = mock(ReportSubscriptionAccessPolicy.class);
        ReportSubscriptionDispatchService dispatch = mock(ReportSubscriptionDispatchService.class);
        OperationLogService logs = mock(OperationLogService.class);
        when(access.currentUser()).thenReturn(CurrentUser.builder().uuid("viewer-1").build());
        when(subscriptions.selectOne(any())).thenReturn(subscription());
        var service = new ReportSubscriptionRunCommandService(subscriptions, runs, access, dispatch, logs);
        return new Fixture(service, runs, dispatch, logs);
    }

    private ReportSubscription subscription() {
        ReportSubscription item = new ReportSubscription();
        item.setUuid("subscription-1");
        item.setSubscriptionName("日报");
        return item;
    }

    private ReportSubscriptionRun run(String uuid, int status) {
        ReportSubscriptionRun run = new ReportSubscriptionRun();
        run.setUuid(uuid);
        run.setSubscriptionUuid("subscription-1");
        run.setRunStatus(status);
        run.setScheduledFor(LocalDateTime.of(2026, 7, 24, 8, 0));
        return run;
    }

    private record Fixture(ReportSubscriptionRunCommandService service,
                           ReportSubscriptionRunMapper runMapper,
                           ReportSubscriptionDispatchService dispatch,
                           OperationLogService logs) {
    }
}

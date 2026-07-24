package com.paper.mes.report.subscription;

import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.subscription.entity.ReportSubscription;
import com.paper.mes.report.subscription.entity.ReportSubscriptionRun;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionMapper;
import com.paper.mes.report.subscription.service.ReportMetricReleaseResolver;
import com.paper.mes.report.subscription.service.ReportSubscriptionCodec;
import com.paper.mes.report.subscription.service.ReportSubscriptionDispatchService;
import com.paper.mes.report.subscription.service.ReportSubscriptionRecipientDispatcher;
import com.paper.mes.report.subscription.service.ReportSubscriptionRunService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportSubscriptionDispatchServiceTest {
    @Test
    void dispatchNow_createsRunWithoutAdvancingSchedule() {
        ReportMetricReleaseResolver releaseResolver = mock(ReportMetricReleaseResolver.class);
        ReportSubscriptionCodec codec = mock(ReportSubscriptionCodec.class);
        ReportSubscriptionRunService runService = mock(ReportSubscriptionRunService.class);
        ReportSubscriptionRecipientDispatcher dispatcher = mock(ReportSubscriptionRecipientDispatcher.class);
        ReportSubscriptionMapper subscriptionMapper = mock(ReportSubscriptionMapper.class);
        ReportSubscriptionDispatchService service = new ReportSubscriptionDispatchService(
                releaseResolver, codec, runService, dispatcher, subscriptionMapper);
        ReportSubscription subscription = subscription();
        ReportSubscriptionRun run = new ReportSubscriptionRun();
        when(releaseResolver.resolve(subscription)).thenReturn("release-1");
        when(codec.read("{}")).thenReturn(new ReportQuery());
        when(runService.start(any(), any())).thenReturn(
                new ReportSubscriptionRunService.StartResult(run, true));
        when(dispatcher.dispatch(any(), any(), any())).thenReturn(
                new ReportSubscriptionRecipientDispatcher.DispatchResult(1, 1, 0, null));

        ReportSubscriptionRun result = service.dispatchNow(subscription,
                LocalDateTime.of(2026, 7, 20, 18, 50, 42));

        assertSame(run, result);
        verify(runService).bindRelease(run, "release-1");
        verify(runService).complete(any(), any());
        verifyNoInteractions(subscriptionMapper);
    }

    @Test
    void dispatchNow_whenReleaseIsUnavailable_recordsFailedRun() {
        ReportMetricReleaseResolver releaseResolver = mock(ReportMetricReleaseResolver.class);
        ReportSubscriptionCodec codec = mock(ReportSubscriptionCodec.class);
        ReportSubscriptionRunService runService = mock(ReportSubscriptionRunService.class);
        ReportSubscriptionRecipientDispatcher dispatcher = mock(ReportSubscriptionRecipientDispatcher.class);
        ReportSubscriptionMapper subscriptionMapper = mock(ReportSubscriptionMapper.class);
        ReportSubscriptionDispatchService service = new ReportSubscriptionDispatchService(
                releaseResolver, codec, runService, dispatcher, subscriptionMapper);
        ReportSubscription subscription = subscription();
        ReportSubscriptionRun run = new ReportSubscriptionRun();
        when(runService.start(any(), any())).thenReturn(
                new ReportSubscriptionRunService.StartResult(run, true));
        when(releaseResolver.resolve(subscription)).thenThrow(new IllegalStateException("release unavailable"));

        ReportSubscriptionRun result = service.dispatchNow(subscription,
                LocalDateTime.of(2026, 7, 20, 18, 51, 10));

        assertSame(run, result);
        verify(runService).fail(any(), any());
        verify(dispatcher, never()).dispatch(any(), any(), any());
        verifyNoInteractions(subscriptionMapper);
    }

    private ReportSubscription subscription() {
        ReportSubscription value = new ReportSubscription();
        value.setUuid("subscription-1");
        value.setTimezone("Asia/Shanghai");
        value.setReportQuery("{}");
        value.setPeriodPolicy(1);
        return value;
    }
}

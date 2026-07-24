package com.paper.mes.report.subscription;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.report.subscription.entity.ReportSubscriptionRun;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionRunMapper;
import com.paper.mes.report.subscription.service.ReportSubscriptionRunService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportSubscriptionRunServiceTest {
    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ReportSubscriptionRun.class);
    }

    @Test
    void start_whenSlotIsRunning_doesNotClaimAgain() {
        ReportSubscriptionRunMapper mapper = mock(ReportSubscriptionRunMapper.class);
        ReportSubscriptionRunService service = new ReportSubscriptionRunService(mapper);
        ReportSubscriptionRun running = run(1);
        when(mapper.selectOne(any())).thenReturn(running);

        var result = service.start("subscription-1", running.getScheduledFor());

        assertSame(running, result.run());
        assertFalse(result.claimed());
        verify(mapper, never()).update(any(), any());
    }

    @Test
    void start_whenSlotIsPartial_claimsForRetry() {
        ReportSubscriptionRunMapper mapper = mock(ReportSubscriptionRunMapper.class);
        ReportSubscriptionRun partial = run(3);
        partial.setMetricReleaseUuid("release-1");
        when(mapper.selectOne(any())).thenReturn(partial);
        when(mapper.update(any(), any())).thenReturn(1);

        var result = new ReportSubscriptionRunService(mapper).start(
                "subscription-1", partial.getScheduledFor());

        assertSame(partial, result.run());
        assertTrue(result.claimed());
        assertSame("release-1", result.run().getMetricReleaseUuid());
    }

    private ReportSubscriptionRun run(int status) {
        ReportSubscriptionRun run = new ReportSubscriptionRun();
        run.setUuid("run-1");
        run.setSubscriptionUuid("subscription-1");
        run.setScheduledFor(LocalDateTime.of(2026, 7, 20, 19, 30));
        run.setRunStatus(status);
        return run;
    }
}

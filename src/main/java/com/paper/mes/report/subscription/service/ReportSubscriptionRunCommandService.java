package com.paper.mes.report.subscription.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.report.subscription.entity.ReportSubscription;
import com.paper.mes.report.subscription.entity.ReportSubscriptionRun;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionMapper;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportSubscriptionRunCommandService {
    private final ReportSubscriptionMapper subscriptionMapper;
    private final ReportSubscriptionRunMapper runMapper;
    private final ReportSubscriptionAccessPolicy accessPolicy;
    private final ReportSubscriptionDispatchService dispatchService;
    private final OperationLogService operationLogService;

    public String runNow(String subscriptionUuid) {
        ReportSubscription subscription = requireOwned(subscriptionUuid);
        var run = dispatchService.dispatchNow(subscription,
                LocalDateTime.now(ReportSubscriptionSchedulePolicy.STORAGE_ZONE));
        operationLogService.record(OperationLogService.BIZ_TYPE_REPORT, subscriptionUuid,
                subscription.getSubscriptionName(), OperationLogService.ACTION_REPORT_SUBSCRIPTION_RUN_NOW,
                null, "手动试跑");
        return run.getUuid();
    }

    public String retry(String subscriptionUuid, String runUuid) {
        ReportSubscription subscription = requireOwned(subscriptionUuid);
        ReportSubscriptionRun run = requireRetryableRun(subscriptionUuid, runUuid);
        var retried = dispatchService.retry(subscription, run.getScheduledFor());
        operationLogService.record(OperationLogService.BIZ_TYPE_REPORT, subscriptionUuid,
                subscription.getSubscriptionName(), OperationLogService.ACTION_REPORT_SUBSCRIPTION_RETRY,
                null, "重试订阅运行 " + runUuid);
        return retried.getUuid();
    }

    private ReportSubscriptionRun requireRetryableRun(String subscriptionUuid, String runUuid) {
        ReportSubscriptionRun run = runMapper.selectById(runUuid);
        if (run == null || !subscriptionUuid.equals(run.getSubscriptionUuid())) {
            throw new BusinessException("订阅运行记录不存在");
        }
        if (!Integer.valueOf(3).equals(run.getRunStatus()) && !Integer.valueOf(4).equals(run.getRunStatus())) {
            throw new BusinessException("仅部分派发或派发失败的运行可以重试");
        }
        return run;
    }

    private ReportSubscription requireOwned(String uuid) {
        ReportSubscription subscription = subscriptionMapper.selectOne(new LambdaQueryWrapper<ReportSubscription>()
                .eq(ReportSubscription::getUuid, uuid)
                .eq(ReportSubscription::getOwnerUuid, accessPolicy.currentUser().getUuid()));
        if (subscription == null) throw new BusinessException("报表订阅不存在");
        return subscription;
    }
}

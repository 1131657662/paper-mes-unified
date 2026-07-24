package com.paper.mes.report.subscription.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.common.BusinessException;
import com.paper.mes.report.subscription.dto.ReportSubscriptionRunPageVO;
import com.paper.mes.report.subscription.dto.ReportSubscriptionRunQuery;
import com.paper.mes.report.subscription.dto.ReportSubscriptionRunVO;
import com.paper.mes.report.subscription.entity.ReportSubscription;
import com.paper.mes.report.subscription.entity.ReportSubscriptionRun;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionMapper;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportSubscriptionRunQueryService {
    private final ReportSubscriptionMapper subscriptionMapper;
    private final ReportSubscriptionRunMapper runMapper;
    private final ReportSubscriptionAccessPolicy accessPolicy;

    public ReportSubscriptionRunPageVO page(String subscriptionUuid, ReportSubscriptionRunQuery query) {
        requireOwned(subscriptionUuid);
        Page<ReportSubscriptionRun> page = runMapper.selectPage(
                new Page<>(current(query), size(query)), runQuery(subscriptionUuid, query));
        return new ReportSubscriptionRunPageVO(page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    private LambdaQueryWrapper<ReportSubscriptionRun> runQuery(
            String subscriptionUuid, ReportSubscriptionRunQuery query) {
        return new LambdaQueryWrapper<ReportSubscriptionRun>()
                .eq(ReportSubscriptionRun::getSubscriptionUuid, subscriptionUuid)
                .eq(query.getRunStatus() != null, ReportSubscriptionRun::getRunStatus, query.getRunStatus())
                .orderByDesc(ReportSubscriptionRun::getScheduledFor)
                .orderByDesc(ReportSubscriptionRun::getUuid);
    }

    private void requireOwned(String uuid) {
        String ownerUuid = accessPolicy.currentUser().getUuid();
        ReportSubscription subscription = subscriptionMapper.selectOne(new LambdaQueryWrapper<ReportSubscription>()
                .eq(ReportSubscription::getUuid, uuid)
                .eq(ReportSubscription::getOwnerUuid, ownerUuid));
        if (subscription == null) throw new BusinessException("报表订阅不存在");
    }

    private ReportSubscriptionRunVO toVO(ReportSubscriptionRun run) {
        return new ReportSubscriptionRunVO(run.getUuid(), run.getScheduledFor(), run.getMetricReleaseUuid(),
                run.getRunStatus(), run.getPlannedCount(), run.getDispatchedCount(), run.getFailedCount(),
                run.getErrorMessage(), run.getCompletedAt());
    }

    private int current(ReportSubscriptionRunQuery query) {
        return query.getCurrent() == null ? 1 : query.getCurrent();
    }

    private int size(ReportSubscriptionRunQuery query) {
        return query.getSize() == null ? 10 : query.getSize();
    }
}

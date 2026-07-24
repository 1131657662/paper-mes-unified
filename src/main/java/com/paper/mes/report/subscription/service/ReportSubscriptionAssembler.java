package com.paper.mes.report.subscription.service;

import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.report.subscription.dto.ReportSubscriptionRecipientVO;
import com.paper.mes.report.subscription.dto.ReportSubscriptionVO;
import com.paper.mes.report.subscription.entity.ReportSubscription;
import com.paper.mes.report.subscription.entity.ReportSubscriptionRecipient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReportSubscriptionAssembler {

    private final ReportSubscriptionCodec codec;

    public ReportSubscriptionVO toVO(ReportSubscription subscription,
                                     Map<String, List<ReportSubscriptionRecipient>> recipients,
                                     Map<String, SysUser> users) {
        List<ReportSubscriptionRecipientVO> recipientVOs = recipients
                .getOrDefault(subscription.getUuid(), List.of()).stream()
                .map(item -> toRecipientVO(users.get(item.getRecipientUuid())))
                .filter(java.util.Objects::nonNull)
                .toList();
        return new ReportSubscriptionVO(subscription.getUuid(), subscription.getSubscriptionName(),
                subscription.getReportPath(),
                subscription.getScheduleType(), subscription.getExecutionTime(), subscription.getWeekDay(),
                subscription.getMonthDay(), subscription.getTimezone(), codec.read(subscription.getReportQuery()),
                subscription.getPeriodPolicy(), releasePolicy(subscription), subscription.getPinnedReleaseUuid(),
                subscription.getIsEnabled(), subscription.getNextRunAt(),
                subscription.getLastScheduledAt(), subscription.getLastErrorMessage(), subscription.getVersion(),
                recipientVOs);
    }

    private Integer releasePolicy(ReportSubscription subscription) {
        return subscription.getReleasePolicy() == null ? 1 : subscription.getReleasePolicy();
    }

    public ReportSubscriptionRecipientVO toRecipientVO(SysUser user) {
        if (user == null) return null;
        String displayName = user.getRealName() == null || user.getRealName().isBlank()
                ? user.getUsername() : user.getRealName();
        return new ReportSubscriptionRecipientVO(user.getUuid(), user.getUsername(), displayName);
    }
}

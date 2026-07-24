package com.paper.mes.report.subscription.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.exporttask.service.ExportTaskCreationService;
import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.subscription.entity.ReportSubscription;
import com.paper.mes.report.subscription.entity.ReportSubscriptionRecipient;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionRecipientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportSubscriptionRecipientDispatcher {
    private final ReportSubscriptionRecipientMapper recipientMapper;
    private final SysUserMapper userMapper;
    private final ReportSubscriptionAccessPolicy accessPolicy;
    private final ExportTaskCreationService taskCreationService;

    public DispatchResult dispatch(ReportSubscription subscription, LocalDateTime scheduledFor,
                                   ReportQuery query) {
        List<ReportSubscriptionRecipient> recipients = recipientMapper.selectList(
                new LambdaQueryWrapper<ReportSubscriptionRecipient>()
                        .eq(ReportSubscriptionRecipient::getSubscriptionUuid, subscription.getUuid()));
        Map<String, SysUser> users = loadUsers(recipients);
        int dispatched = 0;
        int failed = 0;
        String lastError = null;
        for (ReportSubscriptionRecipient recipient : recipients) {
            SysUser user = users.get(recipient.getRecipientUuid());
            if (!accessPolicy.isEligible(user)) {
                failed++;
                lastError = "接收人已停用或失去报表权限";
                continue;
            }
            try {
                createTask(subscription, scheduledFor, query, user);
                dispatched++;
            } catch (RuntimeException exception) {
                failed++;
                lastError = safeMessage(exception);
            }
        }
        return new DispatchResult(recipients.size(), dispatched, failed, lastError);
    }

    private Map<String, SysUser> loadUsers(List<ReportSubscriptionRecipient> recipients) {
        Set<String> ids = recipients.stream().map(ReportSubscriptionRecipient::getRecipientUuid)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getUuid, Function.identity()));
    }

    private void createTask(ReportSubscription subscription, LocalDateTime scheduledFor,
                            ReportQuery query, SysUser user) {
        String requestId = ReportSubscriptionRequestId.generate(
                subscription.getUuid(), scheduledFor, user.getUuid());
        taskCreationService.createScheduledReportTask(requestId, subscription.getUuid(),
                subscription.getSubscriptionName(), subscription.getReportPath(), query, currentUser(user));
    }

    private CurrentUser currentUser(SysUser user) {
        return CurrentUser.builder().uuid(user.getUuid()).username(user.getUsername())
                .realName(user.getRealName()).roleCode(user.getRoleCode()).build();
    }

    private String safeMessage(RuntimeException exception) {
        if (!(exception instanceof BusinessException)) return "导出任务创建失败";
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "导出任务创建失败"
                : message.substring(0, Math.min(500, message.length()));
    }

    public record DispatchResult(int planned, int dispatched, int failed, String errorMessage) {
    }
}

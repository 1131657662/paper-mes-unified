package com.paper.mes.report.subscription.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.report.dto.ReportQuery;
import com.paper.mes.report.subscription.dto.ReportSubscriptionRecipientVO;
import com.paper.mes.report.subscription.dto.ReportSubscriptionSaveDTO;
import com.paper.mes.report.subscription.dto.ReportSubscriptionVO;
import com.paper.mes.report.subscription.entity.ReportSubscription;
import com.paper.mes.report.subscription.entity.ReportSubscriptionRecipient;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionMapper;
import com.paper.mes.report.subscription.mapper.ReportSubscriptionRecipientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportSubscriptionService {
    private final ReportSubscriptionMapper subscriptionMapper;
    private final ReportSubscriptionRecipientMapper recipientMapper;
    private final SysUserMapper userMapper;
    private final ReportSubscriptionAccessPolicy accessPolicy;
    private final ReportSubscriptionAssembler assembler;
    private final ReportSubscriptionCodec codec;
    private final ReportMetricReleaseResolver releaseResolver;
    private final OperationLogService operationLogService;

    public List<ReportSubscriptionVO> listMine() {
        CurrentUser user = accessPolicy.currentUser();
        List<ReportSubscription> subscriptions = subscriptionMapper.selectList(
                new LambdaQueryWrapper<ReportSubscription>()
                        .eq(ReportSubscription::getOwnerUuid, user.getUuid())
                        .orderByDesc(ReportSubscription::getIsEnabled)
                        .orderByAsc(ReportSubscription::getNextRunAt));
        if (subscriptions.isEmpty()) return List.of();
        return assemble(subscriptions);
    }

    public List<ReportSubscriptionRecipientVO> recipientCandidates() {
        return accessPolicy.eligibleRecipients(accessPolicy.currentUser()).stream()
                .map(assembler::toRecipientVO).toList();
    }

    @Transactional
    public String create(ReportSubscriptionSaveDTO dto) {
        CurrentUser actor = accessPolicy.currentUser();
        List<SysUser> recipients = accessPolicy.resolveRecipients(actor, dto.getRecipientUuids());
        validateReleasePolicy(dto);
        ReportSubscription subscription = new ReportSubscription();
        subscription.setOwnerUuid(actor.getUuid());
        apply(subscription, dto);
        try {
            ConcurrencyGuard.requireRowUpdated(subscriptionMapper.insert(subscription));
            replaceRecipients(subscription.getUuid(), recipients);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("订阅名称已存在");
        }
        record(subscription, "新增报表订阅");
        return subscription.getUuid();
    }

    @Transactional
    public void update(String uuid, ReportSubscriptionSaveDTO dto) {
        if (dto.getVersion() == null) throw new BusinessException("更新订阅必须携带数据版本");
        CurrentUser actor = accessPolicy.currentUser();
        ReportSubscription subscription = findOwned(uuid, actor.getUuid());
        List<SysUser> recipients = accessPolicy.resolveRecipients(actor, dto.getRecipientUuids());
        validateReleasePolicy(dto);
        apply(subscription, dto);
        subscription.setVersion(dto.getVersion());
        try {
            ConcurrencyGuard.requireRowUpdated(subscriptionMapper.updateById(subscription));
            replaceRecipients(subscription.getUuid(), recipients);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("订阅名称已存在");
        }
        record(subscription, "修改报表订阅");
    }

    @Transactional
    public void delete(String uuid, int version) {
        CurrentUser actor = accessPolicy.currentUser();
        findOwned(uuid, actor.getUuid());
        int rows = subscriptionMapper.update(null, new LambdaUpdateWrapper<ReportSubscription>()
                .eq(ReportSubscription::getUuid, uuid)
                .eq(ReportSubscription::getOwnerUuid, actor.getUuid())
                .eq(ReportSubscription::getVersion, version)
                .set(ReportSubscription::getIsDeleted, 1)
                .setSql("version = version + 1"));
        ConcurrencyGuard.requireRowUpdated(rows);
        operationLogService.record("报表订阅", uuid, null, "删除报表订阅", null, "删除报表订阅");
    }

    private void apply(ReportSubscription target, ReportSubscriptionSaveDTO dto) {
        target.setSubscriptionName(dto.getSubscriptionName().trim());
        target.setReportPath(dto.getReportPath().trim());
        target.setScheduleType(dto.getScheduleType());
        target.setExecutionTime(dto.getExecutionTime());
        target.setWeekDay(dto.getWeekDay());
        target.setMonthDay(dto.getMonthDay());
        target.setTimezone(dto.getTimezone());
        target.setReportQuery(codec.write(subscriptionQuery(dto)));
        target.setPeriodPolicy(dto.getPeriodPolicy());
        target.setReleasePolicy(dto.getReleasePolicy());
        target.setPinnedReleaseUuid(trim(dto.getPinnedReleaseUuid()));
        target.setDeliveryChannel("DOWNLOAD_CENTER");
        target.setIsEnabled(dto.getIsEnabled());
        target.setNextRunAt(ReportSubscriptionSchedulePolicy.nextRun(target,
                LocalDateTime.now(ReportSubscriptionSchedulePolicy.STORAGE_ZONE)));
        target.setLastErrorMessage(null);
    }

    private ReportQuery subscriptionQuery(ReportSubscriptionSaveDTO dto) {
        ReportQuery query = codec.read(codec.write(dto.getReportQuery()));
        query.setMetricReleaseUuid(null);
        return query;
    }

    private void validateReleasePolicy(ReportSubscriptionSaveDTO dto) {
        if (Integer.valueOf(2).equals(dto.getReleasePolicy())) {
            releaseResolver.requirePinned(dto.getPinnedReleaseUuid());
        }
    }

    private void replaceRecipients(String subscriptionUuid, List<SysUser> users) {
        recipientMapper.delete(new LambdaQueryWrapper<ReportSubscriptionRecipient>()
                .eq(ReportSubscriptionRecipient::getSubscriptionUuid, subscriptionUuid));
        users.forEach(user -> recipientMapper.insert(recipient(subscriptionUuid, user.getUuid())));
    }

    private ReportSubscriptionRecipient recipient(String subscriptionUuid, String userUuid) {
        ReportSubscriptionRecipient recipient = new ReportSubscriptionRecipient();
        recipient.setSubscriptionUuid(subscriptionUuid);
        recipient.setRecipientUuid(userUuid);
        return recipient;
    }

    private String trim(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ReportSubscription findOwned(String uuid, String ownerUuid) {
        ReportSubscription subscription = subscriptionMapper.selectOne(
                new LambdaQueryWrapper<ReportSubscription>()
                        .eq(ReportSubscription::getUuid, uuid)
                        .eq(ReportSubscription::getOwnerUuid, ownerUuid));
        if (subscription == null) throw new BusinessException("报表订阅不存在");
        return subscription;
    }

    private List<ReportSubscriptionVO> assemble(List<ReportSubscription> subscriptions) {
        Set<String> ids = subscriptions.stream().map(ReportSubscription::getUuid).collect(Collectors.toSet());
        List<ReportSubscriptionRecipient> recipients = recipientMapper.selectList(
                new LambdaQueryWrapper<ReportSubscriptionRecipient>()
                        .in(ReportSubscriptionRecipient::getSubscriptionUuid, ids));
        Map<String, List<ReportSubscriptionRecipient>> bySubscription = recipients.stream()
                .collect(Collectors.groupingBy(ReportSubscriptionRecipient::getSubscriptionUuid));
        Set<String> userIds = recipients.stream().map(ReportSubscriptionRecipient::getRecipientUuid)
                .collect(Collectors.toSet());
        Map<String, SysUser> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getUuid, Function.identity()));
        return subscriptions.stream().map(item -> assembler.toVO(item, bySubscription, users)).toList();
    }

    private void record(ReportSubscription subscription, String action) {
        operationLogService.record("报表订阅", subscription.getUuid(), subscription.getSubscriptionName(),
                action, null, action);
    }
}

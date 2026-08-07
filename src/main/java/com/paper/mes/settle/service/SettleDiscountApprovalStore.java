package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.settle.entity.SettleDiscountApproval;
import com.paper.mes.settle.mapper.SettleDiscountApprovalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SettleDiscountApprovalStore {
    private final SettleDiscountApprovalMapper mapper;

    public void insert(SettleDiscountApproval item) {
        ConcurrencyGuard.requireRowUpdated(mapper.insert(item));
    }

    public SettleDiscountApproval findByRequestId(String settleUuid, String requestId) {
        return mapper.selectOne(new LambdaQueryWrapper<SettleDiscountApproval>()
                .eq(SettleDiscountApproval::getSettleUuid, settleUuid)
                .eq(SettleDiscountApproval::getRequestId, requestId.trim()));
    }

    public SettleDiscountApproval findActive(String settleUuid) {
        return mapper.selectOne(new LambdaQueryWrapper<SettleDiscountApproval>()
                .eq(SettleDiscountApproval::getSettleUuid, settleUuid)
                .in(SettleDiscountApproval::getApprovalStatus,
                        SettleDiscountApprovalStatus.PENDING, SettleDiscountApprovalStatus.APPROVED)
                .last("ORDER BY request_time DESC LIMIT 1 FOR UPDATE"));
    }

    public SettleDiscountApproval load(String uuid) {
        SettleDiscountApproval item = mapper.selectById(uuid);
        if (item == null) throw notFound();
        return item;
    }

    public SettleDiscountApproval lock(String uuid) {
        SettleDiscountApproval item = mapper.selectOne(new LambdaQueryWrapper<SettleDiscountApproval>()
                .eq(SettleDiscountApproval::getUuid, uuid).last("FOR UPDATE"));
        if (item == null) throw notFound();
        return item;
    }

    public SettleDiscountApproval lockPending(String settleUuid, String uuid) {
        SettleDiscountApproval item = mapper.selectOne(new LambdaQueryWrapper<SettleDiscountApproval>()
                .eq(SettleDiscountApproval::getUuid, uuid)
                .eq(SettleDiscountApproval::getSettleUuid, settleUuid).last("FOR UPDATE"));
        if (item == null) throw notFound();
        if (!Integer.valueOf(SettleDiscountApprovalStatus.PENDING).equals(item.getApprovalStatus())) {
            throw new BusinessException("优惠审批已处理，不能重复操作");
        }
        return item;
    }

    public void approve(String uuid, CurrentUser user, String reason) {
        ConcurrencyGuard.requireRowUpdated(mapper.update(null, pendingUpdate(uuid)
                .set(SettleDiscountApproval::getApprovalStatus, SettleDiscountApprovalStatus.APPROVED)
                .set(SettleDiscountApproval::getApproveBy, user.getUuid())
                .set(SettleDiscountApproval::getApproveByName, displayName(user))
                .set(SettleDiscountApproval::getApproveTime, LocalDateTime.now())
                .set(SettleDiscountApproval::getDecisionReason, clean(reason))));
    }

    public void decide(String uuid, int status, CurrentUser user, String reason) {
        ConcurrencyGuard.requireRowUpdated(mapper.update(null, pendingUpdate(uuid)
                .set(SettleDiscountApproval::getApprovalStatus, status)
                .set(SettleDiscountApproval::getApproveBy, user.getUuid())
                .set(SettleDiscountApproval::getApproveByName, displayName(user))
                .set(SettleDiscountApproval::getApproveTime, LocalDateTime.now())
                .set(SettleDiscountApproval::getDecisionReason, reason.trim())));
    }

    public void cancel(String uuid, CurrentUser user, String reason) {
        ConcurrencyGuard.requireRowUpdated(mapper.update(null,
                new LambdaUpdateWrapper<SettleDiscountApproval>()
                        .eq(SettleDiscountApproval::getUuid, uuid)
                        .in(SettleDiscountApproval::getApprovalStatus,
                                SettleDiscountApprovalStatus.PENDING, SettleDiscountApprovalStatus.APPROVED)
                        .set(SettleDiscountApproval::getApprovalStatus, SettleDiscountApprovalStatus.CANCELLED)
                        .set(SettleDiscountApproval::getCancelBy, user.getUuid())
                        .set(SettleDiscountApproval::getCancelByName, displayName(user))
                        .set(SettleDiscountApproval::getCancelTime, LocalDateTime.now())
                        .set(SettleDiscountApproval::getDecisionReason, clean(reason))
                        .setSql("version = version + 1")));
    }

    public void markStale(String uuid, String reason) {
        mapper.update(null, new LambdaUpdateWrapper<SettleDiscountApproval>()
                .eq(SettleDiscountApproval::getUuid, uuid)
                .in(SettleDiscountApproval::getApprovalStatus,
                        SettleDiscountApprovalStatus.PENDING, SettleDiscountApprovalStatus.APPROVED)
                .set(SettleDiscountApproval::getApprovalStatus, SettleDiscountApprovalStatus.STALE)
                .set(SettleDiscountApproval::getDecisionReason, reason)
                .setSql("version = version + 1"));
    }

    private LambdaUpdateWrapper<SettleDiscountApproval> pendingUpdate(String uuid) {
        return new LambdaUpdateWrapper<SettleDiscountApproval>()
                .eq(SettleDiscountApproval::getUuid, uuid)
                .eq(SettleDiscountApproval::getApprovalStatus, SettleDiscountApprovalStatus.PENDING)
                .setSql("version = version + 1");
    }

    private String displayName(CurrentUser user) {
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BusinessException notFound() {
        return new BusinessException(ErrorCode.E002, "优惠审批记录不存在");
    }
}

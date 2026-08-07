package com.paper.mes.settle.service;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.settle.dto.SettleDiscountApprovalRequestDTO;
import com.paper.mes.settle.entity.SettleDiscountApproval;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettleDiscountApprovalService {
    public static final String POLICY_VERSION = "discount-v2";
    private static final int SETTLE_VOID = 4;

    private final SettleOrderMapper settleOrderMapper;
    private final PermissionChecker permissionChecker;
    private final BusinessLockService businessLockService;
    private final SettleDiscountApprovalNotificationService notificationService;
    private final SettleDiscountApprovalRequestFactory requestFactory;
    private final SettleDiscountApprovalStore approvalStore;
    private final OperationLogService operationLogService;

    @Transactional(rollbackFor = Exception.class)
    public String request(String settleUuid, SettleDiscountApprovalRequestDTO dto) {
        permissionChecker.require(Permissions.SETTLE_DISCOUNT);
        businessLockService.lockSettleOrder(settleUuid);
        String requestHash = SettleDiscountApprovalFingerprint.of(settleUuid, dto);
        SettleDiscountApproval duplicate = approvalStore.findByRequestId(settleUuid, dto.getRequestId());
        if (duplicate != null) return requireSameRequest(duplicate, requestHash);
        SettleOrder settle = requireActiveSettle(settleUuid);
        SettleDiscountApproval candidate = requestFactory.create(settle, dto, currentUser());
        SettleDiscountApproval active = approvalStore.findActive(settleUuid);
        if (active != null && candidate.getRequestHash().equals(active.getRequestHash())) return active.getUuid();
        if (active != null) markStale(active, settle, "申请人修改了收款方案");
        SettleDiscountApproval created = insertApproval(settle, dto, candidate);
        notificationService.publishRequested(created, settle);
        record(settle, OperationLogService.ACTION_DISCOUNT_REQUEST,
                "优惠 " + SettleDiscountApprovalRequestFactory.money(created.getDiscountAmount())
                        + "，审批级别 " + created.getRequiredLevel());
        return created.getUuid();
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void approve(String settleUuid, String approvalUuid, String reason) {
        businessLockService.lockSettleOrder(settleUuid);
        SettleOrder settle = requireActiveSettle(settleUuid);
        SettleDiscountApproval approval = approvalStore.lockPending(settleUuid, approvalUuid);
        requireApprovalPermission(approval);
        CurrentUser approver = currentUser();
        requireIndependentApprover(approval, approver);
        requireCurrentSnapshot(approval, settle);
        approvalStore.approve(approval.getUuid(), approver, reason);
        approval.setApprovalStatus(SettleDiscountApprovalStatus.APPROVED);
        approval.setApproveByName(displayName(approver));
        approval.setDecisionReason(clean(reason));
        notificationService.publishDecision(approval, settle, SettleDiscountApprovalStatus.APPROVED);
        record(settle, OperationLogService.ACTION_DISCOUNT_APPROVE, approval.getDecisionReason());
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void reject(String approvalUuid, String reason) {
        requireReason(reason);
        SettleDiscountApproval approval = approvalStore.load(approvalUuid);
        businessLockService.lockSettleOrder(approval.getSettleUuid());
        SettleOrder settle = requireActiveSettle(approval.getSettleUuid());
        approval = approvalStore.lockPending(approval.getSettleUuid(), approvalUuid);
        requireApprovalPermission(approval);
        requireIndependentApprover(approval, currentUser());
        requireCurrentSnapshot(approval, settle);
        updateDecision(approval, SettleDiscountApprovalStatus.REJECTED, reason);
        notificationService.publishDecision(approval, settle, SettleDiscountApprovalStatus.REJECTED);
        record(settle, OperationLogService.ACTION_DISCOUNT_REJECT, reason.trim());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(String approvalUuid, String reason) {
        permissionChecker.require(Permissions.SETTLE_DISCOUNT);
        SettleDiscountApproval approval = approvalStore.load(approvalUuid);
        businessLockService.lockSettleOrder(approval.getSettleUuid());
        approval = approvalStore.lock(approvalUuid);
        CurrentUser user = currentUser();
        if (!user.getUuid().equals(approval.getRequestBy())) {
            throw new BusinessException("只能取消本人提交的优惠审批申请");
        }
        if (!List.of(SettleDiscountApprovalStatus.PENDING, SettleDiscountApprovalStatus.APPROVED)
                .contains(approval.getApprovalStatus())) {
            throw new BusinessException("该优惠审批已处理，不能取消");
        }
        approvalStore.cancel(approvalUuid, user, reason);
        SettleOrder settle = settleOrderMapper.selectById(approval.getSettleUuid());
        record(settle, OperationLogService.ACTION_DISCOUNT_CANCEL, clean(reason));
    }

    private SettleDiscountApproval insertApproval(SettleOrder settle, SettleDiscountApprovalRequestDTO dto,
                                                    SettleDiscountApproval item) {
        try {
            approvalStore.insert(item);
            return item;
        } catch (DuplicateKeyException exception) {
            SettleDiscountApproval duplicate = approvalStore.findByRequestId(settle.getUuid(), dto.getRequestId());
            if (duplicate == null) throw new BusinessException("该结算单已有活动中的优惠审批申请");
            requireSameRequest(duplicate, item.getRequestHash());
            return duplicate;
        }
    }

    private void requireApprovalPermission(SettleDiscountApproval approval) {
        if (SettlementDiscountApprovalLevel.ADMIN.name().equals(approval.getRequiredLevel())) {
            permissionChecker.require(Permissions.SETTLE_DISCOUNT_ADMIN_APPROVE);
        } else {
            permissionChecker.require(Permissions.SETTLE_DISCOUNT_APPROVE,
                    Permissions.SETTLE_DISCOUNT_ADMIN_APPROVE);
        }
    }

    private void requireCurrentSnapshot(SettleDiscountApproval approval, SettleOrder settle) {
        if (SettleDiscountApprovalRequestFactory.money(approval.getUnreceivedSnapshot())
                .compareTo(SettleDiscountApprovalRequestFactory.money(settle.getUnreceivedAmount())) == 0) return;
        markStale(approval, settle, "结算单未收金额已变化");
        throw new BusinessException("未收金额已变化，该优惠审批已失效，请重新提交");
    }

    private void markStale(SettleDiscountApproval approval, SettleOrder settle, String reason) {
        approvalStore.markStale(approval.getUuid(), reason);
        approval.setApprovalStatus(SettleDiscountApprovalStatus.STALE);
        approval.setDecisionReason(reason);
        notificationService.publishDecision(approval, settle, SettleDiscountApprovalStatus.STALE);
    }

    private void updateDecision(SettleDiscountApproval approval, int status, String reason) {
        CurrentUser user = currentUser();
        approvalStore.decide(approval.getUuid(), status, user, reason);
        approval.setApprovalStatus(status);
        approval.setApproveByName(displayName(user));
        approval.setDecisionReason(reason.trim());
    }

    private String requireSameRequest(SettleDiscountApproval item, String requestHash) {
        if (item == null || !requestHash.equals(item.getRequestHash())) {
            throw new BusinessException("请求号已用于其他优惠审批申请");
        }
        return item.getUuid();
    }

    private SettleOrder requireActiveSettle(String uuid) {
        SettleOrder settle = settleOrderMapper.selectById(uuid);
        if (settle == null) throw new BusinessException(ErrorCode.E002, "结算单不存在");
        if (Integer.valueOf(SETTLE_VOID).equals(settle.getSettleStatus())) {
            throw new BusinessException("结算单已作废，不能办理优惠审批");
        }
        return settle;
    }

    private void requireIndependentApprover(SettleDiscountApproval approval, CurrentUser approver) {
        if (approval.getRequestBy().equals(approver.getUuid())) {
            throw new BusinessException("优惠申请人与审批人不能是同一账号");
        }
    }

    private void requireReason(String reason) {
        if (!StringUtils.hasText(reason)) throw new BusinessException("驳回原因不能为空");
    }

    private CurrentUser currentUser() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null) throw new BusinessException("当前登录账号不存在");
        return user;
    }

    private String displayName(CurrentUser user) {
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void record(SettleOrder settle, String action, String remark) {
        if (settle != null) operationLogService.record(OperationLogService.BIZ_TYPE_SETTLE,
                settle.getUuid(), settle.getSettleNo(), action, null, remark);
    }
}

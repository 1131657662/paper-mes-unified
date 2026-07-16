package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.settle.dto.SettleDiscountApprovalRequestDTO;
import com.paper.mes.settle.dto.SettleDiscountApprovalVO;
import com.paper.mes.settle.entity.SettleDiscountApproval;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleDiscountApprovalMapper;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettleDiscountApprovalService {
    private static final int PENDING = 1;
    private static final int APPROVED = 2;
    private static final int SETTLE_VOID = 4;

    private final SettleDiscountApprovalMapper approvalMapper;
    private final SettleOrderMapper settleOrderMapper;
    private final SettlementDiscountSettings settings;
    private final PermissionChecker permissionChecker;
    private final BusinessLockService businessLockService;

    public List<SettleDiscountApprovalVO> list(String settleUuid) {
        return approvalMapper.selectList(new LambdaQueryWrapper<SettleDiscountApproval>()
                        .eq(SettleDiscountApproval::getSettleUuid, settleUuid)
                        .orderByDesc(SettleDiscountApproval::getRequestTime))
                .stream().map(SettleDiscountApprovalVO::from).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public String request(String settleUuid, SettleDiscountApprovalRequestDTO dto) {
        permissionChecker.require(Permissions.SETTLE_DISCOUNT);
        businessLockService.lockSettleOrder(settleUuid);
        SettleOrder settle = requireActiveSettle(settleUuid);
        SettleDiscountApproval existing = findByRequestId(settleUuid, dto.getRequestId());
        if (existing != null) return requireSameRequest(existing, dto);
        settings.requireAllowed(dto.getDiscountAmount(), settle.getUnreceivedAmount());
        if (!settings.requiresApproval(dto.getDiscountAmount())) {
            throw new BusinessException("该优惠金额在免审阈值内，可直接登记收款");
        }
        return insertApproval(settleUuid, dto, currentUser());
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(String settleUuid, String approvalUuid) {
        permissionChecker.require(Permissions.SETTLE_DISCOUNT_APPROVE);
        businessLockService.lockSettleOrder(settleUuid);
        requireActiveSettle(settleUuid);
        SettleDiscountApproval approval = lockApproval(settleUuid, approvalUuid);
        CurrentUser approver = currentUser();
        requireIndependentApprover(approval, approver);
        ConcurrencyGuard.requireRowUpdated(approvalMapper.update(null, approvalUpdate(approvalUuid, approver)));
    }

    private String insertApproval(String settleUuid, SettleDiscountApprovalRequestDTO dto, CurrentUser user) {
        SettleDiscountApproval item = new SettleDiscountApproval();
        item.setSettleUuid(settleUuid);
        item.setRequestId(dto.getRequestId().trim());
        item.setDiscountAmount(money(dto.getDiscountAmount()));
        item.setReason(dto.getReason().trim());
        item.setApprovalStatus(PENDING);
        item.setRequestBy(user.getUuid());
        item.setRequestByName(displayName(user));
        item.setRequestTime(LocalDateTime.now());
        try {
            ConcurrencyGuard.requireRowUpdated(approvalMapper.insert(item));
            return item.getUuid();
        } catch (DuplicateKeyException exception) {
            return requireSameRequest(findByRequestId(settleUuid, dto.getRequestId()), dto);
        }
    }

    private LambdaUpdateWrapper<SettleDiscountApproval> approvalUpdate(String uuid, CurrentUser user) {
        return new LambdaUpdateWrapper<SettleDiscountApproval>()
                .eq(SettleDiscountApproval::getUuid, uuid)
                .eq(SettleDiscountApproval::getApprovalStatus, PENDING)
                .set(SettleDiscountApproval::getApprovalStatus, APPROVED)
                .set(SettleDiscountApproval::getApproveBy, user.getUuid())
                .set(SettleDiscountApproval::getApproveByName, displayName(user))
                .set(SettleDiscountApproval::getApproveTime, LocalDateTime.now())
                .setSql("version = version + 1");
    }

    private SettleOrder requireActiveSettle(String uuid) {
        SettleOrder settle = settleOrderMapper.selectById(uuid);
        if (settle == null) throw new BusinessException(ErrorCode.E002, "结算单不存在");
        if (Integer.valueOf(SETTLE_VOID).equals(settle.getSettleStatus())) {
            throw new BusinessException("结算单已作废，不可申请优惠");
        }
        return settle;
    }

    private SettleDiscountApproval findByRequestId(String settleUuid, String requestId) {
        return approvalMapper.selectOne(new LambdaQueryWrapper<SettleDiscountApproval>()
                .eq(SettleDiscountApproval::getSettleUuid, settleUuid)
                .eq(SettleDiscountApproval::getRequestId, requestId.trim()));
    }

    private SettleDiscountApproval lockApproval(String settleUuid, String approvalUuid) {
        SettleDiscountApproval item = approvalMapper.selectOne(new LambdaQueryWrapper<SettleDiscountApproval>()
                .eq(SettleDiscountApproval::getUuid, approvalUuid)
                .eq(SettleDiscountApproval::getSettleUuid, settleUuid).last("FOR UPDATE"));
        if (item == null) throw new BusinessException(ErrorCode.E002, "优惠审批记录不存在");
        if (!Integer.valueOf(PENDING).equals(item.getApprovalStatus())) {
            throw new BusinessException("优惠审批已处理，不可重复批准");
        }
        return item;
    }

    private String requireSameRequest(SettleDiscountApproval item, SettleDiscountApprovalRequestDTO dto) {
        if (item == null || money(item.getDiscountAmount()).compareTo(money(dto.getDiscountAmount())) != 0
                || !item.getReason().equals(dto.getReason().trim())) {
            throw new BusinessException("请求号已用于其他优惠申请");
        }
        return item.getUuid();
    }

    private void requireIndependentApprover(SettleDiscountApproval approval, CurrentUser approver) {
        if (approval.getRequestBy().equals(approver.getUuid())) {
            throw new BusinessException("优惠申请人与审批人不能是同一账号");
        }
    }

    private CurrentUser currentUser() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null) throw new BusinessException("当前登录账号不存在");
        return user;
    }

    private String displayName(CurrentUser user) {
        return user.getRealName() == null || user.getRealName().isBlank() ? user.getUsername() : user.getRealName();
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}

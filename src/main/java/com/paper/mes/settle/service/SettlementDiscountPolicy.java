package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.settle.dto.ReceiveDTO;
import com.paper.mes.settle.entity.SettleDiscountApproval;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleDiscountApprovalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class SettlementDiscountPolicy {
    private static final int APPROVED = 2;
    private static final int USED = 3;

    private final PermissionChecker permissionChecker;
    private final SettlementDiscountSettings settings;
    private final SettleDiscountApprovalMapper approvalMapper;

    public Decision authorize(SettleOrder settle, ReceiveDTO dto, BigDecimal discount, String operator) {
        if (discount.signum() <= 0) return Decision.none();
        permissionChecker.require(Permissions.SETTLE_DISCOUNT);
        requireReason(dto.getDiscountReason());
        settings.requireAllowed(discount, settle.getUnreceivedAmount());
        if (!settings.requiresApproval(discount)) return Decision.auto(operator);
        SettleDiscountApproval approval = lockApproval(dto.getDiscountApprovalUuid());
        requireApproved(approval, settle.getUuid(), discount);
        return new Decision(approval.getUuid(), approval.getApproveByName());
    }

    public void consume(Decision decision, String receiveUuid) {
        if (decision.approvalUuid() == null) return;
        ConcurrencyGuard.requireRowUpdated(approvalMapper.update(null,
                new LambdaUpdateWrapper<SettleDiscountApproval>()
                        .eq(SettleDiscountApproval::getUuid, decision.approvalUuid())
                        .eq(SettleDiscountApproval::getApprovalStatus, APPROVED)
                        .isNull(SettleDiscountApproval::getUsedReceiveUuid)
                        .set(SettleDiscountApproval::getApprovalStatus, USED)
                        .set(SettleDiscountApproval::getUsedReceiveUuid, receiveUuid)
                        .setSql("version = version + 1")));
    }

    private SettleDiscountApproval lockApproval(String uuid) {
        if (!StringUtils.hasText(uuid)) throw new BusinessException("超过免审阈值，请先完成优惠审批");
        return approvalMapper.selectOne(new LambdaQueryWrapper<SettleDiscountApproval>()
                .eq(SettleDiscountApproval::getUuid, uuid.trim()).last("FOR UPDATE"));
    }

    private void requireApproved(SettleDiscountApproval approval, String settleUuid, BigDecimal discount) {
        if (approval == null || !settleUuid.equals(approval.getSettleUuid())) {
            throw new BusinessException("优惠审批记录不存在");
        }
        if (approval.getApprovalStatus() == null || approval.getApprovalStatus() != APPROVED) {
            throw new BusinessException("优惠审批尚未批准或已被使用");
        }
        if (money(approval.getDiscountAmount()).compareTo(money(discount)) != 0) {
            throw new BusinessException("优惠金额与审批金额不一致");
        }
    }

    private void requireReason(String reason) {
        if (!StringUtils.hasText(reason)) throw new BusinessException("优惠/尾差核销必须填写原因");
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    public record Decision(String approvalUuid, String approvedBy) {
        static Decision none() { return new Decision(null, null); }
        static Decision auto(String operator) { return new Decision(null, operator); }
    }
}

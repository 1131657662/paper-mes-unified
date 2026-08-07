package com.paper.mes.settle.service;

import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.settle.dto.SettleDiscountApprovalRequestDTO;
import com.paper.mes.settle.entity.SettleDiscountApproval;
import com.paper.mes.settle.entity.SettleOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SettleDiscountApprovalRequestFactory {
    private final SettlementDiscountSettings settings;

    public SettleDiscountApproval create(SettleOrder settle, SettleDiscountApprovalRequestDTO dto,
                                          CurrentUser user) {
        validate(settle, dto);
        SettleDiscountApproval item = new SettleDiscountApproval();
        item.setSettleUuid(settle.getUuid());
        item.setRequestId(dto.getRequestId().trim());
        item.setCashAmount(money(dto.getCashAmount()));
        item.setScrapOffsetAmount(money(dto.getScrapOffsetAmount()));
        item.setDiscountAmount(money(dto.getDiscountAmount()));
        item.setUnreceivedSnapshot(money(dto.getUnreceivedSnapshot()));
        item.setDiscountPercent(settings.discountPercent(dto.getDiscountAmount(), dto.getUnreceivedSnapshot()));
        item.setRequiredLevel(settings.approvalLevel(dto.getDiscountAmount(), dto.getUnreceivedSnapshot()).name());
        item.setRequestHash(SettleDiscountApprovalFingerprint.of(settle.getUuid(), dto));
        item.setReason(dto.getReason().trim());
        item.setApprovalStatus(SettleDiscountApprovalStatus.PENDING);
        item.setRequestBy(user.getUuid());
        item.setRequestByName(displayName(user));
        item.setRequestTime(LocalDateTime.now());
        item.setPolicyVersion(SettleDiscountApprovalService.POLICY_VERSION);
        return item;
    }

    private void validate(SettleOrder settle, SettleDiscountApprovalRequestDTO dto) {
        BigDecimal snapshot = money(dto.getUnreceivedSnapshot());
        if (snapshot.compareTo(money(settle.getUnreceivedAmount())) != 0) {
            throw new BusinessException("未收金额已变化，请核对最新金额后重新提交审批");
        }
        BigDecimal total = money(dto.getCashAmount()).add(money(dto.getScrapOffsetAmount()))
                .add(money(dto.getDiscountAmount()));
        if (total.signum() <= 0 || total.compareTo(snapshot) > 0) {
            throw new BusinessException("实际到账、废纸抵扣和优惠合计必须大于0且不能超过未收金额");
        }
        settings.requireAllowed(dto.getDiscountAmount(), snapshot);
        if (settings.approvalLevel(dto.getDiscountAmount(), snapshot) == SettlementDiscountApprovalLevel.DIRECT) {
            throw new BusinessException("该优惠金额在免审阈值内，可直接登记收款");
        }
    }

    static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String displayName(CurrentUser user) {
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }
}

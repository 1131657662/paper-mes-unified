package com.paper.mes.settle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.notification.entity.SystemNotification;
import com.paper.mes.notification.mapper.SystemNotificationMapper;
import com.paper.mes.settle.entity.SettleDiscountApproval;
import com.paper.mes.settle.entity.SettleOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettleDiscountApprovalNotificationService {
    public static final String SOURCE_TYPE = "SETTLE_DISCOUNT_APPROVAL";
    private static final String REQUESTED = "SETTLE_DISCOUNT_REQUESTED";

    private final SystemNotificationMapper notificationMapper;
    private final SysUserMapper userMapper;

    public void publishRequested(SettleDiscountApproval approval, SettleOrder settle) {
        List<SysUser> recipients = eligibleApprovers(approval.getRequiredLevel()).stream()
                .filter(user -> !user.getUuid().equals(approval.getRequestBy()))
                .toList();
        if (recipients.isEmpty()) {
            throw new BusinessException(noApproverMessage(approval.getRequiredLevel()));
        }
        recipients.forEach(user -> upsert(user.getUuid(), REQUESTED, approval.getUuid(),
                        "结算优惠待审批",
                        description(settle, approval) + "，请完成审批。", "WARNING"));
    }

    public void publishDecision(SettleDiscountApproval approval, SettleOrder settle, int status) {
        String type = switch (status) {
            case SettleDiscountApprovalStatus.APPROVED -> "SETTLE_DISCOUNT_APPROVED";
            case SettleDiscountApprovalStatus.REJECTED -> "SETTLE_DISCOUNT_REJECTED";
            case SettleDiscountApprovalStatus.STALE -> "SETTLE_DISCOUNT_STALE";
            default -> null;
        };
        if (type == null) return;
        String title = switch (status) {
            case SettleDiscountApprovalStatus.APPROVED -> "结算优惠审批已通过";
            case SettleDiscountApprovalStatus.REJECTED -> "结算优惠审批已驳回";
            default -> "结算优惠审批已失效";
        };
        upsert(approval.getRequestBy(), type, approval.getUuid(), title,
                description(settle, approval), status == SettleDiscountApprovalStatus.APPROVED ? "INFO" : "WARNING");
    }

    private List<SysUser> eligibleApprovers(String requiredLevel) {
        LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1);
        if (SettlementDiscountApprovalLevel.ADMIN.name().equals(requiredLevel)) {
            query.eq(SysUser::getRoleCode, "admin");
        } else {
            query.in(SysUser::getRoleCode, List.of("admin", "finance"));
        }
        return userMapper.selectList(query);
    }

    private void upsert(String recipientUuid, String type, String sourceUuid,
                        String title, String content, String severity) {
        SystemNotification item = notificationMapper.selectOne(new LambdaQueryWrapper<SystemNotification>()
                .eq(SystemNotification::getRecipientUuid, recipientUuid)
                .eq(SystemNotification::getNotificationType, type)
                .eq(SystemNotification::getSourceUuid, sourceUuid));
        if (item == null) {
            item = new SystemNotification();
            item.setRecipientUuid(recipientUuid);
            item.setNotificationType(type);
            item.setSourceType(SOURCE_TYPE);
            item.setSourceUuid(sourceUuid);
        }
        item.setTitle(title);
        item.setContent(content);
        item.setSeverity(severity);
        item.setReadAt(null);
        if (item.getUuid() == null) notificationMapper.insert(item);
        else notificationMapper.updateById(item);
    }

    private String description(SettleOrder settle, SettleDiscountApproval approval) {
        String settleNo = settle == null ? approval.getSettleUuid() : settle.getSettleNo();
        String customer = settle == null ? "" : " · " + settle.getCustomerName();
        return settleNo + customer + " · 优惠 ¥" + approval.getDiscountAmount().toPlainString();
    }

    private String noApproverMessage(String requiredLevel) {
        return SettlementDiscountApprovalLevel.ADMIN.name().equals(requiredLevel)
                ? "当前没有其他可审批的管理员账号，请先启用另一管理员账号"
                : "当前没有其他可审批的财务或管理员账号，请先启用审批账号";
    }
}

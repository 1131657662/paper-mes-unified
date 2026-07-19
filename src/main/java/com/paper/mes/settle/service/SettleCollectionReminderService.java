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
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.settle.dto.SettleCollectionReminderRequestDTO;
import com.paper.mes.settle.dto.SettleCollectionReminderVO;
import com.paper.mes.settle.entity.SettleCollectionReminder;
import com.paper.mes.settle.entity.SettleOrder;
import com.paper.mes.settle.mapper.SettleCollectionReminderMapper;
import com.paper.mes.settle.mapper.SettleOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SettleCollectionReminderService {
    private final SettleCollectionReminderMapper reminderMapper;
    private final SettleOrderMapper settleOrderMapper;
    private final PermissionChecker permissionChecker;
    private final BusinessLockService businessLockService;
    private final OperationLogService operationLogService;

    public List<SettleCollectionReminderVO> list(String settleUuid) {
        return reminderMapper.selectList(new LambdaQueryWrapper<SettleCollectionReminder>()
                        .eq(SettleCollectionReminder::getSettleUuid, settleUuid)
                        .orderByDesc(SettleCollectionReminder::getReminderTime)
                        .orderByDesc(SettleCollectionReminder::getUuid))
                .stream().map(SettleCollectionReminderVO::from).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public String record(String settleUuid, SettleCollectionReminderRequestDTO dto) {
        permissionChecker.require(Permissions.SETTLE_RECEIVE);
        businessLockService.lockSettleOrder(settleUuid);
        SettleOrder settle = requireReceivable(settleUuid);
        SettleCollectionReminder existing = findByRequestId(settleUuid, dto.getRequestId());
        if (existing != null) return requireSameRequest(existing, dto);
        SettleCollectionReminder reminder = buildReminder(settleUuid, dto, currentUser());
        try {
            ConcurrencyGuard.requireRowUpdated(reminderMapper.insert(reminder));
        } catch (DuplicateKeyException exception) {
            return requireSameRequest(findByRequestId(settleUuid, dto.getRequestId()), dto);
        }
        updateSettleSnapshot(settleUuid, reminder);
        operationLogService.record(OperationLogService.BIZ_TYPE_SETTLE, settleUuid, settle.getSettleNo(),
                OperationLogService.ACTION_COLLECTION_REMINDER, reminder.getOperatorName(), reminder.getRemark());
        return reminder.getUuid();
    }

    private SettleCollectionReminder buildReminder(String settleUuid, SettleCollectionReminderRequestDTO dto,
                                                    CurrentUser user) {
        SettleCollectionReminder item = new SettleCollectionReminder();
        item.setSettleUuid(settleUuid);
        item.setRequestId(dto.getRequestId().trim());
        item.setReminderChannel(dto.getReminderChannel());
        item.setReminderResult(dto.getReminderResult());
        item.setContactName(trimToNull(dto.getContactName()));
        item.setReminderTime(dto.getReminderTime() == null ? LocalDateTime.now() : dto.getReminderTime());
        item.setNextFollowUpDate(dto.getNextFollowUpDate());
        item.setOperatorUuid(user.getUuid());
        item.setOperatorName(displayName(user));
        item.setRemark(dto.getRemark().trim());
        return item;
    }

    private void updateSettleSnapshot(String settleUuid, SettleCollectionReminder reminder) {
        LambdaUpdateWrapper<SettleOrder> update = new LambdaUpdateWrapper<SettleOrder>()
                .eq(SettleOrder::getUuid, settleUuid)
                .in(SettleOrder::getSettleStatus, 1, 2)
                .set(SettleOrder::getLastReminderTime, reminder.getReminderTime())
                .set(SettleOrder::getLastReminderBy, reminder.getOperatorName())
                .set(SettleOrder::getLastReminderResult, reminder.getReminderResult())
                .set(SettleOrder::getNextFollowUpDate, reminder.getNextFollowUpDate())
                .setSql("reminder_count = reminder_count + 1, version = version + 1");
        ConcurrencyGuard.requireRowUpdated(settleOrderMapper.update(null, update));
    }

    private SettleOrder requireReceivable(String uuid) {
        SettleOrder settle = settleOrderMapper.selectById(uuid);
        if (settle == null) throw new BusinessException(ErrorCode.E002, "结算单不存在");
        if (!List.of(1, 2).contains(settle.getSettleStatus()) || settle.getUnreceivedAmount().signum() <= 0) {
            throw new BusinessException("结算单已结清或已作废，无需催收");
        }
        return settle;
    }

    private SettleCollectionReminder findByRequestId(String settleUuid, String requestId) {
        return reminderMapper.selectOne(new LambdaQueryWrapper<SettleCollectionReminder>()
                .eq(SettleCollectionReminder::getSettleUuid, settleUuid)
                .eq(SettleCollectionReminder::getRequestId, requestId.trim()));
    }

    private String requireSameRequest(SettleCollectionReminder item, SettleCollectionReminderRequestDTO dto) {
        if (item == null || !item.getReminderChannel().equals(dto.getReminderChannel())
                || !item.getReminderResult().equals(dto.getReminderResult())
                || !Objects.equals(item.getContactName(), trimToNull(dto.getContactName()))
                || !Objects.equals(item.getNextFollowUpDate(), dto.getNextFollowUpDate())
                || dto.getReminderTime() != null && !dto.getReminderTime().equals(item.getReminderTime())
                || !item.getRemark().equals(dto.getRemark().trim())) {
            throw new BusinessException("请求号已用于其他催收记录");
        }
        return item.getUuid();
    }

    private CurrentUser currentUser() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null) throw new BusinessException("当前登录账号不存在");
        return user;
    }

    private String displayName(CurrentUser user) {
        return user.getRealName() == null || user.getRealName().isBlank() ? user.getUsername() : user.getRealName();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

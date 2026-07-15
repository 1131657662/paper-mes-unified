package com.paper.mes.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.backup.service.BackupTaskFailedEvent;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ResultCode;
import com.paper.mes.health.dto.DataHealthIssueVO;
import com.paper.mes.notification.dto.NotificationSummaryVO;
import com.paper.mes.notification.dto.SystemNotificationVO;
import com.paper.mes.notification.entity.SystemNotification;
import com.paper.mes.notification.mapper.SystemNotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SystemNotificationService {

    private static final String BACKUP_FAILED = "BACKUP_FAILED";
    private static final String DATA_HEALTH_ALERT = "DATA_HEALTH_ALERT";
    private static final String SOURCE_BACKUP_TASK = "BACKUP_TASK";
    private static final String SOURCE_DATA_HEALTH = "DATA_HEALTH";
    private static final Set<String> NOTIFIABLE_WARNINGS = Set.of(
            "OVERDUE_BACK_RECORD", "OVERDUE_RECEIVABLE");
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9A-Za-z-]{1,36}");
    private final SystemNotificationMapper notificationMapper;
    private final SysUserMapper userMapper;

    public NotificationSummaryVO currentUserSummary() {
        String recipientUuid = currentUserUuid();
        long unreadCount = notificationMapper.selectCount(recipientQuery(recipientUuid).isNull(SystemNotification::getReadAt));
        List<SystemNotificationVO> items = notificationMapper.selectList(recipientQuery(recipientUuid)
                        .orderByDesc(SystemNotification::getCreateTime).last("LIMIT 20"))
                .stream().map(SystemNotificationVO::from).toList();
        return new NotificationSummaryVO(unreadCount, items);
    }

    public void markRead(String notificationUuid) {
        requireValidUuid(notificationUuid);
        SystemNotification notification = findCurrentUserNotification(notificationUuid);
        if (notification.getReadAt() != null) return;
        notification.setReadAt(LocalDateTime.now());
        ConcurrencyGuard.requireRowUpdated(notificationMapper.updateById(notification));
    }

    public void markAllRead() {
        notificationMapper.update(null, new LambdaUpdateWrapper<SystemNotification>()
                .eq(SystemNotification::getRecipientUuid, currentUserUuid())
                .isNull(SystemNotification::getReadAt)
                .set(SystemNotification::getReadAt, LocalDateTime.now()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishBackupFailure(BackupTaskFailedEvent event) {
        activeAdministrators().forEach(user -> notificationMapper.insert(notification(user, event)));
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized void publishDataHealthIssues(List<DataHealthIssueVO> issues) {
        List<DataHealthIssueVO> criticalIssues = issues.stream()
                .filter(this::isNotifiable)
                .filter(issue -> issue.businessUuid() != null && !issue.businessUuid().isBlank())
                .collect(Collectors.toMap(DataHealthIssueVO::businessUuid, Function.identity(), (first, ignored) -> first))
                .values().stream()
                .toList();
        if (criticalIssues.isEmpty()) return;
        activeAdministrators().forEach(user -> publishHealthIssues(user, criticalIssues));
    }

    private boolean isNotifiable(DataHealthIssueVO issue) {
        return "CRITICAL".equals(issue.severity()) || NOTIFIABLE_WARNINGS.contains(issue.issueType());
    }

    private List<SysUser> activeAdministrators() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleCode, "admin")
                .eq(SysUser::getStatus, 1));
    }

    private SystemNotification notification(SysUser user, BackupTaskFailedEvent event) {
        SystemNotification notification = new SystemNotification();
        notification.setRecipientUuid(user.getUuid());
        notification.setNotificationType(BACKUP_FAILED);
        notification.setSeverity("ERROR");
        notification.setTitle(failureTitle(event.taskType()));
        notification.setContent("数据安全任务执行失败，请在系统配置的数据安全页查看任务历史。");
        notification.setSourceType(SOURCE_BACKUP_TASK);
        notification.setSourceUuid(event.taskUuid());
        return notification;
    }

    private void publishHealthIssues(SysUser user, List<DataHealthIssueVO> issues) {
        List<String> sourceUuids = issues.stream().map(DataHealthIssueVO::businessUuid).toList();
        Set<String> existingSources = notificationMapper.selectList(new LambdaQueryWrapper<SystemNotification>()
                .select(SystemNotification::getSourceUuid)
                .eq(SystemNotification::getRecipientUuid, user.getUuid())
                .eq(SystemNotification::getNotificationType, DATA_HEALTH_ALERT)
                .in(SystemNotification::getSourceUuid, sourceUuids))
                .stream().map(SystemNotification::getSourceUuid).collect(Collectors.toSet());
        issues.stream()
                .filter(issue -> !existingSources.contains(issue.businessUuid()))
                .forEach(issue -> notificationMapper.insert(healthNotification(user, issue)));
    }

    private SystemNotification healthNotification(SysUser user, DataHealthIssueVO issue) {
        SystemNotification notification = new SystemNotification();
        notification.setRecipientUuid(user.getUuid());
        notification.setNotificationType(DATA_HEALTH_ALERT);
        notification.setSeverity("CRITICAL".equals(issue.severity()) ? "ERROR" : "WARNING");
        notification.setTitle(issue.title());
        notification.setContent(issue.businessType() + " " + issue.businessNo() + "：" + issue.detail());
        notification.setSourceType(SOURCE_DATA_HEALTH);
        notification.setSourceUuid(issue.businessUuid());
        return notification;
    }

    private String failureTitle(String taskType) {
        return switch (taskType) {
            case "AUTO_BACKUP" -> "自动备份失败";
            case "VERIFY" -> "恢复演练失败";
            default -> "手动备份失败";
        };
    }

    private SystemNotification findCurrentUserNotification(String notificationUuid) {
        SystemNotification notification = notificationMapper.selectOne(recipientQuery(currentUserUuid())
                .eq(SystemNotification::getUuid, notificationUuid));
        if (notification == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
        }
        return notification;
    }

    private LambdaQueryWrapper<SystemNotification> recipientQuery(String recipientUuid) {
        return new LambdaQueryWrapper<SystemNotification>()
                .eq(SystemNotification::getRecipientUuid, recipientUuid);
    }

    private String currentUserUuid() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null || user.getUuid().isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录状态已失效");
        }
        return user.getUuid();
    }

    private void requireValidUuid(String uuid) {
        if (uuid == null || !UUID_PATTERN.matcher(uuid).matches()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "通知编号格式不正确");
        }
    }
}

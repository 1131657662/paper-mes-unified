package com.paper.mes.exporttask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.exporttask.config.ExportTaskRuntimeProperties;
import com.paper.mes.notification.entity.SystemNotification;
import com.paper.mes.notification.mapper.SystemNotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ExportTaskStorageAlertService {
    static final String ALERT_KEY = "EXPORT_TASK_STORAGE";
    static final String NOTIFICATION_TYPE = "EXPORT_STORAGE_ALERT";
    static final String SOURCE_TYPE = "EXPORT_STORAGE";

    private final JdbcTemplate jdbcTemplate;
    private final SysUserMapper userMapper;
    private final SystemNotificationMapper notificationMapper;
    private final ExportTaskStorage storage;
    private final long minimumFreeBytes;
    private final double minimumFreePercent;

    public ExportTaskStorageAlertService(JdbcTemplate jdbcTemplate, SysUserMapper userMapper,
                                         SystemNotificationMapper notificationMapper,
                                         ExportTaskStorage storage,
                                         ExportTaskRuntimeProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.userMapper = userMapper;
        this.notificationMapper = notificationMapper;
        this.storage = storage;
        this.minimumFreeBytes = properties.getStorageMinFreeBytes();
        this.minimumFreePercent = properties.getStorageMinFreePercent();
    }

    @Transactional(rollbackFor = Exception.class)
    public void check() {
        ExportTaskStorageHealth health = storage.health(minimumFreeBytes, minimumFreePercent);
        ensureStateRow();
        StorageAlertState state = currentState();
        ExportTaskStorageAlertTransition transition = ExportTaskStorageAlertTransition.evaluate(
                state.stateCode(), health.status(), state.transitionNo());
        if (!transition.changed()) return;
        updateState(transition);
        if (transition.notificationRequired()) publishNotifications(health, transition.transitionNo());
        log.info("Export task storage state changed: {} -> {}", transition.previousState(),
                transition.currentState());
    }

    private void ensureStateRow() {
        jdbcTemplate.update("""
                INSERT INTO sys_operational_alert_state(alert_key, state_code, transition_no)
                VALUES (?, 'UNKNOWN', 0)
                ON DUPLICATE KEY UPDATE alert_key = VALUES(alert_key)
                """, ALERT_KEY);
    }

    private StorageAlertState currentState() {
        return jdbcTemplate.queryForObject("""
                SELECT state_code, transition_no
                FROM sys_operational_alert_state
                WHERE alert_key = ?
                FOR UPDATE
                """, (resultSet, rowNum) -> new StorageAlertState(
                resultSet.getString("state_code"), resultSet.getLong("transition_no")), ALERT_KEY);
    }

    private void updateState(ExportTaskStorageAlertTransition transition) {
        jdbcTemplate.update("""
                UPDATE sys_operational_alert_state
                SET state_code = ?, transition_no = ?
                WHERE alert_key = ?
                """, transition.currentState(), transition.transitionNo(), ALERT_KEY);
    }

    private void publishNotifications(ExportTaskStorageHealth health, long transitionNo) {
        String sourceUuid = ALERT_KEY + "-" + Long.toUnsignedString(transitionNo, 36);
        List<SysUser> administrators = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleCode, "admin").eq(SysUser::getStatus, 1));
        administrators.forEach(user -> notificationMapper.insert(notification(user, health, sourceUuid)));
    }

    private SystemNotification notification(SysUser user, ExportTaskStorageHealth health,
                                            String sourceUuid) {
        SystemNotification notification = new SystemNotification();
        notification.setUuid(UUID.randomUUID().toString());
        notification.setRecipientUuid(user.getUuid());
        notification.setNotificationType(NOTIFICATION_TYPE);
        boolean ready = ExportTaskStorageHealth.READY.equals(health.status());
        boolean lowSpace = ExportTaskStorageHealth.LOW_SPACE.equals(health.status());
        notification.setSeverity(ready ? "INFO" : lowSpace ? "WARNING" : "ERROR");
        notification.setTitle(ready ? "共享存储已恢复" : "下载任务共享存储异常");
        notification.setContent(ready
                ? "共享存储已恢复可写，新导出任务可正常生成。"
                : "共享存储" + statusDescription(health.status())
                + "，新导出任务已暂停，请在下载任务中心查看运行状态。");
        notification.setSourceType(SOURCE_TYPE);
        notification.setSourceUuid(sourceUuid);
        return notification;
    }

    private String statusDescription(String status) {
        return switch (status) {
            case ExportTaskStorageHealth.LOW_SPACE -> "可用空间低于安全阈值";
            case ExportTaskStorageHealth.READ_ONLY -> "目录只读";
            case ExportTaskStorageHealth.UNAVAILABLE -> "目录不可用";
            default -> "健康检查失败";
        };
    }

    private record StorageAlertState(String stateCode, long transitionNo) {
    }
}

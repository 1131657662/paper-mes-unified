package com.paper.mes.report.alert.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.report.alert.entity.ReportAlertRule;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportAlertNotificationPublisher {
    private static final int BATCH_SIZE = 100;
    private final JdbcTemplate jdbcTemplate;
    private final SysUserMapper userMapper;
    private final PermissionChecker permissionChecker;

    @Transactional
    public void publish(AlertNotification alert) {
        List<SysUser> recipients = reportViewers();
        if (recipients.isEmpty()) return;
        jdbcTemplate.batchUpdate(UPSERT_SQL, recipients, BATCH_SIZE,
                (statement, user) -> setParameters(statement, user, alert));
    }

    private List<SysUser> reportViewers() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1))
                .stream().filter(user -> permissionChecker.hasRolePermission(
                        user.getRoleCode(), Permissions.REPORT_VIEW)).toList();
    }

    private void setParameters(PreparedStatement statement, SysUser user,
                               AlertNotification alert) throws SQLException {
        statement.setString(1, UUID.randomUUID().toString().replace("-", ""));
        statement.setString(2, user.getUuid());
        statement.setString(3, alert.rule().getSeverity() == 2 ? "ERROR" : "WARNING");
        statement.setString(4, title(alert.rule()));
        statement.setString(5, content(alert));
        statement.setString(6, alert.eventUuid());
    }

    private String title(ReportAlertRule rule) {
        String value = (rule.getSeverity() == 2 ? "严重预警：" : "报表预警：") + rule.getRuleName();
        return limit(value, 100);
    }

    private String content(AlertNotification alert) {
        String value = alert.scopeLabel() + "在" + alert.window().periodStart() + "至" + alert.window().asOf()
                + "的" + signalLabel(alert.rule().getSignalCode()) + "为"
                + alert.metricValue().stripTrailingZeros().toPlainString() + "%，阈值为"
                + operatorLabel(alert.rule().getComparisonOperator())
                + alert.rule().getThresholdValue().stripTrailingZeros().toPlainString() + "%";
        return limit(value, 500);
    }

    private String signalLabel(String signalCode) {
        return ReportAlertThresholdService.LOSS_RATIO.equals(signalCode) ? "损耗率" : "已结算未收占比";
    }

    private String operatorLabel(String operator) {
        return switch (operator) {
            case "GT" -> ">";
            case "LT" -> "<";
            case "LTE" -> "≤";
            default -> "≥";
        };
    }

    private String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record AlertNotification(
            String eventUuid,
            ReportAlertRule rule,
            BigDecimal metricValue,
            ReportAlertEvaluationWindow window,
            String scopeLabel
    ) {
    }

    private static final String UPSERT_SQL = """
            INSERT INTO sys_notification
              (uuid, recipient_uuid, notification_type, severity, title, content,
               source_type, source_uuid, read_at)
            VALUES (?, ?, 'REPORT_ALERT', ?, ?, ?, 'REPORT_ALERT_EVENT', ?, NULL)
            ON DUPLICATE KEY UPDATE severity = VALUES(severity), title = VALUES(title),
              content = VALUES(content), source_type = VALUES(source_type), read_at = NULL,
              is_deleted = 0, update_time = CURRENT_TIMESTAMP, version = version + 1
            """;
}

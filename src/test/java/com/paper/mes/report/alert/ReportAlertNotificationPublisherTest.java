package com.paper.mes.report.alert;

import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.report.alert.entity.ReportAlertRule;
import com.paper.mes.report.alert.service.ReportAlertEvaluationWindow;
import com.paper.mes.report.alert.service.ReportAlertNotificationPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportAlertNotificationPublisherTest {
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private PermissionChecker permissionChecker;

    @Test
    void publish_reportViewer_usesSingleBatchWrite() {
        SysUser user = user("admin");
        when(userMapper.selectList(any())).thenReturn(List.of(user));
        when(permissionChecker.hasRolePermission("admin", Permissions.REPORT_VIEW)).thenReturn(true);
        ReportAlertNotificationPublisher publisher = publisher();

        publisher.publish(alert());

        verify(jdbcTemplate).batchUpdate(anyString(), any(Collection.class), eq(100),
                any(ParameterizedPreparedStatementSetter.class));
    }

    @Test
    void publish_userWithoutReportPermission_doesNotWrite() {
        when(userMapper.selectList(any())).thenReturn(List.of(user("guest")));
        when(permissionChecker.hasRolePermission("guest", Permissions.REPORT_VIEW)).thenReturn(false);

        publisher().publish(alert());

        verifyNoInteractions(jdbcTemplate);
    }

    private ReportAlertNotificationPublisher publisher() {
        return new ReportAlertNotificationPublisher(jdbcTemplate, userMapper, permissionChecker);
    }

    private ReportAlertNotificationPublisher.AlertNotification alert() {
        ReportAlertRule rule = new ReportAlertRule();
        rule.setRuleName("损耗率预警");
        rule.setSignalCode("LOSS_RATIO");
        rule.setComparisonOperator("GTE");
        rule.setThresholdValue(new BigDecimal("5"));
        rule.setSeverity(2);
        return new ReportAlertNotificationPublisher.AlertNotification("event-1", rule,
                new BigDecimal("6.2"), ReportAlertEvaluationWindow.currentMonth(
                LocalDate.of(2026, 7, 20)), "全局");
    }

    private SysUser user(String roleCode) {
        SysUser user = new SysUser();
        user.setUuid("user-" + roleCode);
        user.setRoleCode(roleCode);
        return user;
    }
}

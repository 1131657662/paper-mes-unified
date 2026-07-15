package com.paper.mes.notification.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.paper.mes.auth.entity.SysUser;
import com.paper.mes.auth.mapper.SysUserMapper;
import com.paper.mes.health.dto.DataHealthIssueVO;
import com.paper.mes.notification.entity.SystemNotification;
import com.paper.mes.notification.mapper.SystemNotificationMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemNotificationServiceTest {

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), SystemNotification.class);
    }

    @Test
    void publishDataHealthIssues_insertsOnlyNewDistinctCriticalSources() {
        SystemNotificationMapper notificationMapper = mock(SystemNotificationMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SystemNotificationService service = new SystemNotificationService(notificationMapper, userMapper);
        SysUser admin = new SysUser();
        admin.setUuid("admin-1");
        when(userMapper.selectList(any())).thenReturn(List.of(admin));
        when(notificationMapper.selectList(any())).thenReturn(List.of(existing("business-1")));

        service.publishDataHealthIssues(List.of(
                issue("TYPE", "business-1", "CRITICAL", "已存在"),
                issue("TYPE", "business-2", "CRITICAL", "新增"),
                issue("TYPE", "business-2", "CRITICAL", "重复来源"),
                issue("OTHER_WARNING", "business-3", "WARNING", "普通警告"),
                issue("OVERDUE_BACK_RECORD", "business-4", "WARNING", "回录超期")
        ));

        ArgumentCaptor<SystemNotification> captor = ArgumentCaptor.forClass(SystemNotification.class);
        verify(notificationMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(List.of("business-2", "business-4"), captor.getAllValues().stream()
                .map(SystemNotification::getSourceUuid).sorted().toList());
        assertEquals(List.of("ERROR", "WARNING"), captor.getAllValues().stream()
                .map(SystemNotification::getSeverity).sorted().toList());
    }

    private SystemNotification existing(String sourceUuid) {
        SystemNotification notification = new SystemNotification();
        notification.setSourceUuid(sourceUuid);
        return notification;
    }

    private DataHealthIssueVO issue(String type, String uuid, String severity, String title) {
        return new DataHealthIssueVO(type, severity, "加工单", uuid,
                "JG-001", title, "检查详情", null);
    }
}

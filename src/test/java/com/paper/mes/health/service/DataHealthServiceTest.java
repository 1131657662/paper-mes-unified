package com.paper.mes.health.service;

import com.paper.mes.health.dto.DataHealthIssueVO;
import com.paper.mes.notification.service.SystemNotificationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataHealthServiceTest {

    @Test
    void inspect_collectsIssuesFromAllRegisteredInspectors() {
        DataHealthInspector first = mock(DataHealthInspector.class);
        DataHealthInspector second = mock(DataHealthInspector.class);
        SystemNotificationService notifications = mock(SystemNotificationService.class);
        DataHealthIssueVO warning = issue("WARNING", "JG-2");
        DataHealthIssueVO critical = issue("CRITICAL", "JG-1");
        when(first.inspect()).thenReturn(List.of(warning));
        when(second.inspect()).thenReturn(List.of(critical));
        DataHealthService service = new DataHealthService(List.of(first, second), notifications);

        var result = service.inspect();

        assertEquals(1, result.criticalCount());
        assertEquals(1, result.warningCount());
        assertEquals("CRITICAL", result.issues().getFirst().severity());
        verify(notifications).publishDataHealthIssues(result.issues());
    }

    private DataHealthIssueVO issue(String severity, String businessNo) {
        return new DataHealthIssueVO("TEST", severity, "加工单", businessNo, businessNo,
                "测试异常", "测试明细", null);
    }
}

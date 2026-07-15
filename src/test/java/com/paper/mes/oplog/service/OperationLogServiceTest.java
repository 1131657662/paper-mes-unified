package com.paper.mes.oplog.service;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.oplog.entity.OperationLog;
import com.paper.mes.oplog.mapper.OperationLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OperationLogServiceTest {

    private final OperationLogMapper mapper = mock(OperationLogMapper.class);
    private final OperationLogService service = new OperationLogService(mapper);

    @BeforeEach
    void prepareContext() {
        AuthContextHolder.clear();
    }

    @AfterEach
    void clearContext() {
        AuthContextHolder.clear();
    }

    @Test
    void record_whenLoggedIn_ignoresClientSuppliedOperator() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder()
                .uuid("user-1").username("operator").realName("真实登录人").roleCode("operator").build());

        service.record("结算单", "settle-1", "JS001", "收款", "伪造姓名", "登记收款");

        assertThat(captured().getOperator()).isEqualTo("真实登录人");
    }

    @Test
    void recordVerifiedActor_preservesPasswordVerifiedAdministrator() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder()
                .uuid("user-1").username("operator").realName("回录员").roleCode("operator").build());

        service.recordVerifiedActor("加工单", "order-1", "JG001", "超差放行", "授权管理员", "已复核");

        assertThat(captured().getOperator()).isEqualTo("授权管理员");
    }

    @Test
    void record_withoutLogin_usesSystemCallerIdentity() {
        service.record("数据安全", "backup", null, "数据备份", "system", "自动备份");

        assertThat(captured().getOperator()).isEqualTo("system");
    }

    private OperationLog captured() {
        ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
        verify(mapper).insert(captor.capture());
        return captor.getValue();
    }
}

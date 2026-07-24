package com.paper.mes.report.savedview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.report.savedview.mapper.ReportSavedViewMapper;
import com.paper.mes.report.savedview.service.ReportSavedViewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportSavedViewServiceSecurityTest {
    private final ReportSavedViewMapper mapper = mock(ReportSavedViewMapper.class);
    private final ReportSavedViewService service = new ReportSavedViewService(mapper,
            new ObjectMapper().findAndRegisterModules(), mock(OperationLogService.class));

    @AfterEach
    void clearAuth() { AuthContextHolder.clear(); }

    @Test
    void delete_otherUsersView_returnsNotFound() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user-a").roleCode("viewer").build());
        when(mapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.delete("view-owned-by-b", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("报表视图不存在");
    }
}

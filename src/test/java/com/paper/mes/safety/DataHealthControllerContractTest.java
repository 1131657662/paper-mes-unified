package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.health.controller.DataHealthController;
import com.paper.mes.health.dto.DataHealthRepairRequest;
import com.paper.mes.health.dto.DataHealthRepairResultVO;
import com.paper.mes.health.dto.DataHealthSummaryVO;
import com.paper.mes.health.service.DataHealthRepairService;
import com.paper.mes.health.service.DataHealthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DataHealthControllerContractTest {

    private static final String TOKEN = "test-token";
    private AuthService authService;
    private DataHealthRepairService repairService;
    private DataHealthService healthService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        repairService = mock(DataHealthRepairService.class);
        healthService = mock(DataHealthService.class);
        mvc = MockMvcBuilders.standaloneSetup(new DataHealthController(healthService, repairService))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void inspect_withoutToken_returnsUnauthorized() throws Exception {
        mvc.perform(get("/api/system/data-health"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        verify(healthService, never()).inspect();
    }

    @Test
    void inspect_withOperatorRole_returnsForbidden() throws Exception {
        authorizeAs("operator");
        mvc.perform(get("/api/system/data-health").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        verify(healthService, never()).inspect();
    }

    @Test
    void inspect_withAdminRole_returnsSummary() throws Exception {
        authorizeAs("admin");
        when(healthService.inspect()).thenReturn(new DataHealthSummaryVO(LocalDateTime.now(), 1, 2, List.of()));
        mvc.perform(get("/api/system/data-health").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.criticalCount").value(1));
    }

    @Test
    void repair_withoutReason_returnsBadRequest() throws Exception {
        authorizeAs("admin");
        mvc.perform(post("/api/system/data-health/settlements/settlement-1/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\",\"confirmation\":\"JS202607010001\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        verify(repairService, never()).reconcileSettlement(any(), any());
    }

    @Test
    void repair_withAdminRole_bindsConfirmation() throws Exception {
        authorizeAs("admin");
        when(repairService.reconcileSettlement(eq("settlement-1"), any()))
                .thenReturn(new DataHealthRepairResultVO("JS202607010001", "ok"));
        mvc.perform(post("/api/system/data-health/settlements/settlement-1/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"本地测试修复\",\"confirmation\":\"JS202607010001\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DataHealthRepairRequest> captor = ArgumentCaptor.forClass(DataHealthRepairRequest.class);
        verify(repairService).reconcileSettlement(eq("settlement-1"), captor.capture());
        assertEquals("JS202607010001", captor.getValue().confirmation());
    }

    private void authorizeAs(String roleCode) {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("user-uuid")
                .username("tester")
                .realName("tester")
                .roleCode(roleCode)
                .build());
    }
}

package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.report.alert.controller.ReportAlertRuleController;
import com.paper.mes.report.alert.service.ReportAlertRuleService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportAlertRuleControllerContractTest {
    private AuthService authService;
    private ReportAlertRuleService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        service = mock(ReportAlertRuleService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ReportAlertRuleController(service))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void list_withViewerRole_returnsForbidden() throws Exception {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn("token");
        when(authService.currentUser("token")).thenReturn(CurrentUser.builder()
                .uuid("viewer").roleCode("viewer").build());

        mvc.perform(get("/api/report-alert-rules").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(service, never()).list();
    }
}

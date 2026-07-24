package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.report.alert.controller.ReportAlertEventController;
import com.paper.mes.report.alert.dto.ReportAlertEventPageVO;
import com.paper.mes.report.alert.service.ReportAlertEventService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportAlertEventControllerContractTest {
    private AuthService authService;
    private ReportAlertEventService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        service = mock(ReportAlertEventService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ReportAlertEventController(service))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void list_withViewerRole_returnsEvents() throws Exception {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn("token");
        when(authService.currentUser("token")).thenReturn(CurrentUser.builder()
                .uuid("viewer").roleCode("viewer").build());
        when(service.page(any())).thenReturn(new ReportAlertEventPageVO(List.of(), 0, 1, 20, 0, 0, 0));

        mvc.perform(get("/api/report-alert-events").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(service).page(any());
    }

    @Test
    void acknowledge_withViewerRole_returnsForbidden() throws Exception {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn("token");
        when(authService.currentUser("token")).thenReturn(CurrentUser.builder()
                .uuid("viewer").roleCode("viewer").build());

        mvc.perform(post("/api/report-alert-events/0123456789abcdef0123456789abcdef/acknowledge")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(service, never()).acknowledge(any());
    }
}

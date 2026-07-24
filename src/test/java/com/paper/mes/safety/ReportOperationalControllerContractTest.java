package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.report.controller.ReportOperationalController;
import com.paper.mes.report.service.ReportOperationalAnalysisService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportOperationalControllerContractTest {
    private AuthService authService;
    private ReportOperationalAnalysisService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        service = mock(ReportOperationalAnalysisService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ReportOperationalController(service))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void operationalTopics_withoutTokenAreRejected() throws Exception {
        mvc.perform(post("/api/reports/topics/inventory/query")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        verify(service, never()).inventory(any());
    }

    @Test
    void operationalTopics_withViewerRoleAreAuthorized() throws Exception {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn("token");
        when(authService.currentUser("token")).thenReturn(CurrentUser.builder()
                .uuid("viewer").username("viewer").roleCode("viewer").build());

        mvc.perform(post("/api/reports/topics/delivery/query")
                        .header("Authorization", "Bearer token")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        verify(service).delivery(any());
    }
}

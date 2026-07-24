package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.report.savedview.controller.ReportSavedViewController;
import com.paper.mes.report.savedview.service.ReportSavedViewService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportSavedViewControllerContractTest {
    private static final String TOKEN = "saved-view-token";
    private AuthService authService;
    private ReportSavedViewService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        service = mock(ReportSavedViewService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ReportSavedViewController(service))
                .addInterceptors(new AuthInterceptor(authService), new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void list_withoutToken_returnsUnauthorized() throws Exception {
        mvc.perform(get("/api/report-saved-views")).andExpect(status().isUnauthorized());
        verify(service, never()).listMine();
    }

    @Test
    void list_withReportPermission_returnsOwnedViews() throws Exception {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("viewer-1").username("viewer").roleCode("viewer").build());
        when(service.listMine()).thenReturn(List.of());

        mvc.perform(get("/api/report-saved-views").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
    }
}

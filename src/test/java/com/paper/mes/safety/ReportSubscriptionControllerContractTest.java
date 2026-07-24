package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.report.subscription.controller.ReportSubscriptionController;
import com.paper.mes.report.subscription.service.ReportSubscriptionService;
import com.paper.mes.report.subscription.service.ReportSubscriptionRunCommandService;
import com.paper.mes.report.subscription.service.ReportSubscriptionRunQueryService;
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

class ReportSubscriptionControllerContractTest {
    private static final String TOKEN = "subscription-token";
    private AuthService authService;
    private ReportSubscriptionService subscriptionService;
    private ReportSubscriptionRunQueryService runQueryService;
    private ReportSubscriptionRunCommandService runCommandService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        subscriptionService = mock(ReportSubscriptionService.class);
        runQueryService = mock(ReportSubscriptionRunQueryService.class);
        runCommandService = mock(ReportSubscriptionRunCommandService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ReportSubscriptionController(subscriptionService,
                        runQueryService, runCommandService))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void list_withoutToken_returnsUnauthorized() throws Exception {
        mvc.perform(get("/api/report-subscriptions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verify(subscriptionService, never()).listMine();
    }

    @Test
    void list_withReportPermission_returnsOwnedSubscriptions() throws Exception {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("viewer-1").username("viewer").roleCode("viewer").build());
        when(subscriptionService.listMine()).thenReturn(List.of());

        mvc.perform(get("/api/report-subscriptions").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void runNow_withReportPermission_dispatchesOwnedSubscription() throws Exception {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("viewer-1").username("viewer").roleCode("viewer").build());
        String uuid = "0123456789abcdef0123456789abcdef";
        when(runCommandService.runNow(uuid)).thenReturn("run-uuid");

        mvc.perform(post("/api/report-subscriptions/{uuid}/run-now", uuid)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("run-uuid"));

        verify(runCommandService).runNow(uuid);
    }
}

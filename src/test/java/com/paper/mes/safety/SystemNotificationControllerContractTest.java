package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.notification.controller.SystemNotificationController;
import com.paper.mes.notification.dto.NotificationSummaryVO;
import com.paper.mes.notification.service.SystemNotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemNotificationControllerContractTest {

    private static final String TOKEN = "test-token";
    private SystemNotificationService notificationService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        AuthService authService = mock(AuthService.class);
        notificationService = mock(SystemNotificationService.class);
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("u-admin").username("admin").roleCode("admin").build());
        mvc = MockMvcBuilders.standaloneSetup(new SystemNotificationController(notificationService))
                .addInterceptors(new AuthInterceptor(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void summary_withAuthenticatedUser_returnsUnreadCount() throws Exception {
        when(notificationService.currentUserSummary()).thenReturn(new NotificationSummaryVO(2, List.of()));

        mvc.perform(get("/api/notifications").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.unreadCount").value(2));
    }

    @Test
    void markRead_withValidUuid_bindsCurrentNotification() throws Exception {
        mvc.perform(put("/api/notifications/notification-uuid/read")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(notificationService).markRead("notification-uuid");
    }
}

package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.settle.controller.SettleController;
import com.paper.mes.settle.dto.ReceiveDTO;
import com.paper.mes.settle.dto.SettleActionReasonDTO;
import com.paper.mes.settle.service.SettleService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettleReceiveControllerContractTest {

    private static final String TOKEN = "test-token";

    private AuthService authService;
    private SettleService settleService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        settleService = mock(SettleService.class);
        mvc = MockMvcBuilders.standaloneSetup(new SettleController(settleService))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void receive_withWarehouseRole_returnsForbidden() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(post("/api/settle-orders/settle-uuid/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(receivePayload())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        verify(settleService, never()).receive(any(), any());
    }

    @Test
    void receive_withFinanceRole_bindsPayload() throws Exception {
        authorizeAs("finance");

        mvc.perform(post("/api/settle-orders/settle-uuid/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(receivePayload())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<ReceiveDTO> captor = ArgumentCaptor.forClass(ReceiveDTO.class);
        verify(settleService).receive(eq("settle-uuid"), captor.capture());
        assertEquals(new BigDecimal("100.50"), captor.getValue().getCashAmount());
        assertEquals(LocalDateTime.of(2026, 7, 7, 10, 30), captor.getValue().getReceiveDate());
    }

    @Test
    void cancelReceive_withoutReason_returnsBadRequest() throws Exception {
        authorizeAs("finance");

        mvc.perform(post("/api/settle-orders/settle-uuid/receives/receive-uuid/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(settleService, never()).cancelReceive(any(), any(), any());
    }

    @Test
    void cancelReceive_withFinanceRole_bindsReason() throws Exception {
        authorizeAs("finance");

        mvc.perform(post("/api/settle-orders/settle-uuid/receives/receive-uuid/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"wrong amount\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SettleActionReasonDTO> captor = ArgumentCaptor.forClass(SettleActionReasonDTO.class);
        verify(settleService).cancelReceive(eq("settle-uuid"), eq("receive-uuid"), captor.capture());
        assertEquals("wrong amount", captor.getValue().getReason());
    }

    @Test
    void voidSettle_withFinanceRole_bindsReason() throws Exception {
        authorizeAs("finance");

        mvc.perform(post("/api/settle-orders/settle-uuid/void")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"wrong customer\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SettleActionReasonDTO> captor = ArgumentCaptor.forClass(SettleActionReasonDTO.class);
        verify(settleService).voidSettle(eq("settle-uuid"), captor.capture());
        assertEquals("wrong customer", captor.getValue().getReason());
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

    private String receivePayload() {
        return """
                {"cashAmount":100.50,"scrapOffsetAmount":0,"payMethod":2,"operator":"finance","receiveDate":"2026-07-07T10:30:00"}
                """;
    }
}

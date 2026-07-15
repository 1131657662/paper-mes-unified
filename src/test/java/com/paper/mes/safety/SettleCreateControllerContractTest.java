package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.settle.controller.SettleController;
import com.paper.mes.settle.dto.SettleByMonthDTO;
import com.paper.mes.settle.dto.SettleByOrderDTO;
import com.paper.mes.settle.dto.SettleByOrdersDTO;
import com.paper.mes.settle.service.SettleService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettleCreateControllerContractTest {

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
    void createByOrder_withoutToken_returnsUnauthorized() throws Exception {
        mvc.perform(post("/api/settle-orders/by-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderUuid\":\"order-1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verify(settleService, never()).createByOrder(any());
    }

    @Test
    void createByOrder_withWarehouseRole_returnsForbidden() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(post("/api/settle-orders/by-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderUuid\":\"order-1\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(settleService, never()).createByOrder(any());
    }

    @Test
    void createByOrder_withoutOrderUuid_returnsBadRequest() throws Exception {
        authorizeAs("finance");

        mvc.perform(post("/api/settle-orders/by-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderUuid\":\"\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(settleService, never()).createByOrder(any());
    }

    @Test
    void createByOrder_withFinanceRole_bindsPayloadAndReturnsUuid() throws Exception {
        authorizeAs("finance");
        when(settleService.createByOrder(any())).thenReturn("settle-uuid");

        mvc.perform(post("/api/settle-orders/by-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderUuid":"order-1","settleDate":"2026-07-07","isInvoice":2,"remark":"single"}
                                """)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("settle-uuid"));

        ArgumentCaptor<SettleByOrderDTO> captor = ArgumentCaptor.forClass(SettleByOrderDTO.class);
        verify(settleService).createByOrder(captor.capture());
        assertEquals("order-1", captor.getValue().getOrderUuid());
        assertEquals(LocalDate.of(2026, 7, 7), captor.getValue().getSettleDate());
    }

    @Test
    void createByOrders_withFinanceRole_bindsOrderList() throws Exception {
        authorizeAs("finance");
        when(settleService.createByOrders(any())).thenReturn("batch-settle-uuid");

        mvc.perform(post("/api/settle-orders/by-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderUuids":["order-1","order-2"],"periodStart":"2026-07-01","periodEnd":"2026-07-31"}
                                """)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("batch-settle-uuid"));

        ArgumentCaptor<SettleByOrdersDTO> captor = ArgumentCaptor.forClass(SettleByOrdersDTO.class);
        verify(settleService).createByOrders(captor.capture());
        assertEquals(2, captor.getValue().getOrderUuids().size());
        assertEquals("order-2", captor.getValue().getOrderUuids().get(1));
    }

    @Test
    void createByMonth_withoutPeriodStart_returnsBadRequest() throws Exception {
        authorizeAs("finance");

        mvc.perform(post("/api/settle-orders/by-month")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerUuid\":\"customer-1\",\"periodEnd\":\"2026-07-31\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(settleService, never()).createByMonth(any());
    }

    @Test
    void createByMonth_withFinanceRole_bindsPeriod() throws Exception {
        authorizeAs("finance");
        when(settleService.createByMonth(any())).thenReturn("month-settle-uuid");

        mvc.perform(post("/api/settle-orders/by-month")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerUuid":"customer-1","periodStart":"2026-07-01","periodEnd":"2026-07-31","settleDate":"2026-08-01"}
                                """)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("month-settle-uuid"));

        ArgumentCaptor<SettleByMonthDTO> captor = ArgumentCaptor.forClass(SettleByMonthDTO.class);
        verify(settleService).createByMonth(captor.capture());
        assertEquals("customer-1", captor.getValue().getCustomerUuid());
        assertEquals(LocalDate.of(2026, 7, 31), captor.getValue().getPeriodEnd());
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

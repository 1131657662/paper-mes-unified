package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.delivery.controller.DeliveryController;
import com.paper.mes.delivery.dto.DeliveryAppendItemsDTO;
import com.paper.mes.delivery.dto.DeliveryCancelDTO;
import com.paper.mes.delivery.dto.DeliveryConfirmDTO;
import com.paper.mes.delivery.dto.DeliveryCreateDTO;
import com.paper.mes.delivery.dto.DeliveryRollbackDTO;
import com.paper.mes.delivery.service.DeliveryService;
import com.paper.mes.delivery.service.DeliveryListSummaryService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeliveryControllerContractTest {

    private static final String TOKEN = "test-token";

    private AuthService authService;
    private DeliveryService deliveryService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        deliveryService = mock(DeliveryService.class);
        mvc = MockMvcBuilders.standaloneSetup(new DeliveryController(
                        deliveryService, mock(DeliveryListSummaryService.class)))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createDelivery_withoutToken_returnsUnauthorized() throws Exception {
        mvc.perform(post("/api/delivery-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verify(deliveryService, never()).create(any());
    }

    @Test
    void createDelivery_withOperatorRole_returnsForbidden() throws Exception {
        authorizeAs("operator");

        mvc.perform(post("/api/delivery-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryPayload())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(deliveryService, never()).create(any());
    }

    @Test
    void createDelivery_withoutItems_returnsBadRequest() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(post("/api/delivery-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerUuid":"customer-1","deliveryDate":"2026-07-07","items":[]}
                                """)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(deliveryService, never()).create(any());
    }

    @Test
    void createDelivery_withWarehouseRole_bindsPayloadAndReturnsUuid() throws Exception {
        authorizeAs("warehouse");
        when(deliveryService.create(any())).thenReturn("delivery-uuid");

        mvc.perform(post("/api/delivery-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryPayload())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("delivery-uuid"));

        ArgumentCaptor<DeliveryCreateDTO> captor = ArgumentCaptor.forClass(DeliveryCreateDTO.class);
        verify(deliveryService).create(captor.capture());
        assertEquals("customer-1", captor.getValue().getCustomerUuid());
        assertEquals("warehouse-1", captor.getValue().getWarehouseUuid());
        assertEquals(LocalDate.of(2026, 7, 7), captor.getValue().getDeliveryDate());
        assertEquals("finish-1", captor.getValue().getItems().getFirst().getFinishUuid());
        assertEquals(new BigDecimal("12.50"), captor.getValue().getItems().getFirst().getOutWeight());
    }

    @Test
    void confirmDelivery_withWarehouseRoleAndEmptyBody_usesDefaultDto() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(post("/api/delivery-orders/delivery-uuid/confirm")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DeliveryConfirmDTO> captor = ArgumentCaptor.forClass(DeliveryConfirmDTO.class);
        verify(deliveryService).confirm(eq("delivery-uuid"), captor.capture());
        assertNull(captor.getValue().getSignUser());
    }

    @Test
    void appendDetails_withWarehouseRole_bindsPayload() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(post("/api/delivery-orders/delivery-uuid/details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(appendPayload())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DeliveryAppendItemsDTO> captor = ArgumentCaptor.forClass(DeliveryAppendItemsDTO.class);
        verify(deliveryService).appendDetails(eq("delivery-uuid"), captor.capture());
        assertEquals("finish-2", captor.getValue().getItems().getFirst().getFinishUuid());
    }

    @Test
    void rollbackDelivery_withoutReason_returnsBadRequest() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(post("/api/delivery-orders/delivery-uuid/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(deliveryService, never()).rollback(any(), any());
    }

    @Test
    void rollbackDelivery_withWarehouseRole_bindsReason() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(post("/api/delivery-orders/delivery-uuid/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"signed by mistake\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DeliveryRollbackDTO> captor = ArgumentCaptor.forClass(DeliveryRollbackDTO.class);
        verify(deliveryService).rollback(eq("delivery-uuid"), captor.capture());
        assertEquals("signed by mistake", captor.getValue().getReason());
    }

    @Test
    void cancelPendingDelivery_withoutReason_returnsBadRequest() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(post("/api/delivery-orders/delivery-uuid/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(deliveryService, never()).cancelPending(any(), any());
    }

    @Test
    void cancelPendingDelivery_withWarehouseRole_bindsReason() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(post("/api/delivery-orders/delivery-uuid/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"wrong finish rolls\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DeliveryCancelDTO> captor = ArgumentCaptor.forClass(DeliveryCancelDTO.class);
        verify(deliveryService).cancelPending(eq("delivery-uuid"), captor.capture());
        assertEquals("wrong finish rolls", captor.getValue().getReason());
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

    private String deliveryPayload() {
        return """
                {"customerUuid":"customer-1","warehouseUuid":"warehouse-1","deliveryDate":"2026-07-07","pickerName":"picker","items":[{"finishUuid":"finish-1","outWeight":12.50}]}
                """;
    }

    private String appendPayload() {
        return """
                {"forceRelease":true,"items":[{"finishUuid":"finish-2","outWeight":8.25,"remark":"late add"}]}
                """;
    }
}

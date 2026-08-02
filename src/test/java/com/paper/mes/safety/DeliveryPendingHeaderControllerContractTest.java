package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.delivery.controller.DeliveryPendingHeaderController;
import com.paper.mes.delivery.dto.DeliveryPendingUpdateDTO;
import com.paper.mes.delivery.service.DeliveryPendingHeaderService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeliveryPendingHeaderControllerContractTest {

    private static final String TOKEN = "test-token";

    private AuthService authService;
    private DeliveryPendingHeaderService pendingHeaderService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        pendingHeaderService = mock(DeliveryPendingHeaderService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new DeliveryPendingHeaderController(pendingHeaderService))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void updatePendingDelivery_withoutToken_returnsUnauthorized() throws Exception {
        mvc.perform(put("/api/delivery-orders/delivery-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verify(pendingHeaderService, never()).update(any(), any());
    }

    @Test
    void updatePendingDelivery_withOperatorRole_returnsForbidden() throws Exception {
        authorizeAs("operator");

        performUpdate(validPayload())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(pendingHeaderService, never()).update(any(), any());
    }

    @Test
    void updatePendingDelivery_withoutDate_returnsValidationError() throws Exception {
        authorizeAs("warehouse");

        performUpdate("{\"carNo\":\"浙A12345\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        verify(pendingHeaderService, never()).update(any(), any());
    }

    @Test
    void updatePendingDelivery_withWarehouseRole_bindsEditableFields() throws Exception {
        authorizeAs("warehouse");

        performUpdate(validPayload())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DeliveryPendingUpdateDTO> captor =
                ArgumentCaptor.forClass(DeliveryPendingUpdateDTO.class);
        verify(pendingHeaderService).update(eq("delivery-1"), captor.capture());
        assertThat(captor.getValue().getDeliveryDate()).isEqualTo(LocalDate.of(2026, 7, 29));
        assertThat(captor.getValue().getCarNo()).isEqualTo("浙A12345");
        assertThat(captor.getValue().getContainerNo()).isEqualTo("GX-08");
    }

    private org.springframework.test.web.servlet.ResultActions performUpdate(String payload)
            throws Exception {
        return mvc.perform(put("/api/delivery-orders/delivery-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
                .header("Authorization", "Bearer " + TOKEN));
    }

    private void authorizeAs(String roleCode) {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("user-1")
                .username("tester")
                .realName("tester")
                .roleCode(roleCode)
                .build());
    }

    private String validPayload() {
        return """
                {"deliveryDate":"2026-07-29","receiverCustomerName":"永丰包装","carNo":"浙A12345","containerNo":"GX-08"}
                """;
    }
}

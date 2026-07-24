package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.*;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.delivery.controller.DeliveryCustomerRevisionController;
import com.paper.mes.delivery.dto.*;
import com.paper.mes.delivery.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeliveryCustomerRevisionControllerContractTest {

    private static final String TOKEN = "delivery-customer-token";
    private AuthService authService;
    private DeliveryCustomerRevisionPreviewService previewService;
    private DeliveryCustomerRevisionPublisher publisher;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        previewService = mock(DeliveryCustomerRevisionPreviewService.class);
        publisher = mock(DeliveryCustomerRevisionPublisher.class);
        var controller = new DeliveryCustomerRevisionController(
                previewService, mock(DeliveryCustomerRevisionReader.class), publisher);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void current_withoutToken_isRejected() throws Exception {
        mvc.perform(get("/api/delivery-orders/delivery-1/customer-specs"))
                .andExpect(status().isUnauthorized());
        verify(previewService, never()).current(any());
    }

    @Test
    void preview_withViewerRole_isAuthorized() throws Exception {
        authorize("viewer");
        when(previewService.preview(eq("delivery-1"), any()))
                .thenReturn(new DeliveryCustomerRevisionPreviewVO());
        mvc.perform(post("/api/delivery-orders/delivery-1/customer-specs/preview")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType("application/json").content(validRequest()))
                .andExpect(status().isOk());
    }

    @Test
    void publish_withViewerRole_isForbidden() throws Exception {
        authorize("viewer");
        mvc.perform(post("/api/delivery-orders/delivery-1/customer-specs/revisions")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType("application/json").content(validRequest()))
                .andExpect(status().isForbidden());
        verify(publisher, never()).publish(any(), any());
    }

    @Test
    void publish_withWarehouseRole_isAuthorized() throws Exception {
        authorize("warehouse");
        when(publisher.publish(eq("delivery-1"), any()))
                .thenReturn(new DeliveryCustomerRevisionSummaryVO());
        mvc.perform(post("/api/delivery-orders/delivery-1/customer-specs/revisions")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType("application/json").content(validRequest()))
                .andExpect(status().isOk());
    }

    private void authorize(String roleCode) {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("user-1").username("user").roleCode(roleCode).build());
    }

    private String validRequest() {
        return """
                {"requestId":"request-1","expectedDeliveryVersion":1,"reason":"客户要求",
                 "items":[{"deliveryDetailUuid":"detail-1","expectedDetailVersion":1,
                  "calculationMode":"KEEP","roundingScale":3,
                  "roundingMode":"HALF_UP","zeroPolicy":"SKIP"}]}
                """;
    }
}

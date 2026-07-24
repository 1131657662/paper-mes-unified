package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.processorder.controller.FinishCustomerRevisionController;
import com.paper.mes.processorder.dto.FinishCustomerRevisionPreviewVO;
import com.paper.mes.processorder.dto.FinishCustomerRevisionSummaryVO;
import com.paper.mes.processorder.service.FinishCustomerRevisionPreviewService;
import com.paper.mes.processorder.service.FinishCustomerRevisionPublisher;
import com.paper.mes.processorder.service.FinishCustomerRevisionReader;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FinishCustomerRevisionControllerContractTest {

    private static final String TOKEN = "customer-spec-token";
    private AuthService authService;
    private FinishCustomerRevisionPreviewService previewService;
    private FinishCustomerRevisionPublisher publisher;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        previewService = mock(FinishCustomerRevisionPreviewService.class);
        publisher = mock(FinishCustomerRevisionPublisher.class);
        FinishCustomerRevisionReader reader = mock(FinishCustomerRevisionReader.class);
        var controller = new FinishCustomerRevisionController(previewService, reader, publisher);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void current_withoutToken_isRejected() throws Exception {
        mvc.perform(get("/api/process-orders/order-1/customer-specs"))
                .andExpect(status().isUnauthorized());

        verify(previewService, never()).current(any());
    }

    @Test
    void preview_withViewerRole_isAuthorized() throws Exception {
        authorize("viewer");
        when(previewService.preview(eq("order-1"), any())).thenReturn(new FinishCustomerRevisionPreviewVO());

        mvc.perform(post("/api/process-orders/order-1/customer-specs/preview")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isOk());
    }

    @Test
    void publish_withViewerRole_isForbidden() throws Exception {
        authorize("viewer");

        mvc.perform(post("/api/process-orders/order-1/customer-specs/revisions")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isForbidden());

        verify(publisher, never()).publish(any(), any());
    }

    @Test
    void publish_withOrderClerkRole_isAuthorized() throws Exception {
        authorize("order_clerk");
        when(publisher.publish(eq("order-1"), any())).thenReturn(new FinishCustomerRevisionSummaryVO());

        mvc.perform(post("/api/process-orders/order-1/customer-specs/revisions")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType("application/json")
                        .content(validRequest()))
                .andExpect(status().isOk());
    }

    private void authorize(String roleCode) {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("user-1").username("user").roleCode(roleCode).build());
    }

    private String validRequest() {
        return """
                {"requestId":"request-1","expectedOrderVersion":1,"reason":"客户要求",
                 "items":[{"finishUuid":"finish-1","expectedVersion":1,
                  "calculationMode":"KEEP","roundingScale":3,
                  "roundingMode":"HALF_UP","zeroPolicy":"SKIP"}]}
                """;
    }
}

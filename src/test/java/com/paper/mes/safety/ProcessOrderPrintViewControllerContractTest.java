package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.processorder.controller.ProcessOrderController;
import com.paper.mes.processorder.dto.PrintViewVersion;
import com.paper.mes.processorder.dto.ProcessOrderPrintViewVO;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.processorder.service.ProcessRouteAppendService;
import com.paper.mes.processorder.service.ProcessRouteSaveService;
import com.paper.mes.processorder.service.ProcessStepPricingBatchService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProcessOrderPrintViewControllerContractTest {

    private static final String TOKEN = "print-view-token";

    private AuthService authService;
    private ProcessOrderService processOrderService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        processOrderService = mock(ProcessOrderService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProcessOrderController(
                        processOrderService,
                        mock(ProcessRouteSaveService.class),
                        mock(ProcessRouteAppendService.class),
                        mock(ProcessStepPricingBatchService.class)))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void printView_withoutLogin_returnsUnauthorized() throws Exception {
        mvc.perform(get("/api/process-orders/order-1/print-view"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verify(processOrderService, never()).getPrintView(any(), any());
    }

    @Test
    void printView_withFinanceViewPermission_readsRequestedVersion() throws Exception {
        authorizeFinance();
        ProcessOrderPrintViewVO view = new ProcessOrderPrintViewVO();
        view.setVersion(PrintViewVersion.FINISHED);
        when(processOrderService.getPrintView("order-1", PrintViewVersion.FINISHED)).thenReturn(view);

        mvc.perform(get("/api/process-orders/order-1/print-view")
                        .param("version", "FINISHED")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("FINISHED"));

        verify(processOrderService).getPrintView("order-1", PrintViewVersion.FINISHED);
    }

    private void authorizeFinance() {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("finance-1")
                .username("finance")
                .realName("财务用户")
                .roleCode("finance")
                .build());
    }
}

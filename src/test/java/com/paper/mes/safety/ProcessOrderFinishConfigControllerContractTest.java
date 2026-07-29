package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.processorder.controller.ProcessOrderController;
import com.paper.mes.processorder.dto.FinishConfigBatchSaveDTO;
import com.paper.mes.processorder.dto.FinishConfigSaveDTO;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.processorder.service.ProcessRouteAppendService;
import com.paper.mes.processorder.service.ProcessRouteSaveService;
import com.paper.mes.processorder.service.ProcessStepPricingBatchService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProcessOrderFinishConfigControllerContractTest {

    private static final String TOKEN = "test-token";

    private AuthService authService;
    private ProcessOrderService processOrderService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        processOrderService = mock(ProcessOrderService.class);
        ProcessOrderController controller = new ProcessOrderController(
                processOrderService,
                mock(ProcessRouteSaveService.class),
                mock(ProcessRouteAppendService.class),
                mock(ProcessStepPricingBatchService.class));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authorizeOperator();
    }

    @Test
    void saveFinishConfig_withoutExpectedVersion_rejectsRequest() throws Exception {
        mvc.perform(post("/api/process-orders/order-1/rolls/roll-1/finish-config")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processMode\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(processOrderService, never()).saveFinishConfig(
                any(), any(), any(FinishConfigSaveDTO.class), any());
    }

    @Test
    void saveFinishConfig_withExpectedVersion_forwardsVersion() throws Exception {
        mvc.perform(post("/api/process-orders/order-1/rolls/roll-1/finish-config")
                        .param("expectedVersion", "7")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"processMode\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(processOrderService).saveFinishConfig(
                eq("order-1"), eq("roll-1"), any(FinishConfigSaveDTO.class), eq(7));
    }

    @Test
    void saveFinishConfigBatch_withoutExpectedVersion_rejectsRequest() throws Exception {
        mvc.perform(post("/api/process-orders/order-1/finish-config/batch")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(processOrderService, never()).saveFinishConfigBatch(
                any(), any(FinishConfigBatchSaveDTO.class), any());
    }

    @Test
    void saveFinishConfigBatch_withExpectedVersion_forwardsVersion() throws Exception {
        mvc.perform(post("/api/process-orders/order-1/finish-config/batch")
                        .param("expectedVersion", "8")
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(processOrderService).saveFinishConfigBatch(
                eq("order-1"), any(FinishConfigBatchSaveDTO.class), eq(8));
    }

    private void authorizeOperator() {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("user-1")
                .username("operator")
                .roleCode("operator")
                .build());
    }

    private String batchPayload() {
        return "{\"items\":[{\"rollUuid\":\"roll-1\",\"config\":{\"processMode\":3}}]}";
    }
}

package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.processorder.controller.ProcessOrderController;
import com.paper.mes.processorder.dto.BackRecordDTO;
import com.paper.mes.processorder.dto.BackRecordResultVO;
import com.paper.mes.processorder.dto.PrintDTO;
import com.paper.mes.processorder.dto.PrintResultVO;
import com.paper.mes.processorder.dto.ProcessOrderCreateDTO;
import com.paper.mes.processorder.dto.ProcessStepPricingAdjustmentDTO;
import com.paper.mes.processorder.dto.ProcessStepPricingBatchDTO;
import com.paper.mes.processorder.dto.ProcessStepPricingBatchPreviewVO;
import com.paper.mes.processorder.dto.FeeResultVO;
import com.paper.mes.processorder.service.ProcessOrderService;
import com.paper.mes.processorder.service.ProcessRouteAppendService;
import com.paper.mes.processorder.service.ProcessRouteSaveService;
import com.paper.mes.processorder.service.ProcessStepPricingBatchService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProcessOrderControllerContractTest {

    private static final String TOKEN = "test-token";

    private AuthService authService;
    private ProcessOrderService processOrderService;
    private ProcessStepPricingBatchService pricingBatchService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        processOrderService = mock(ProcessOrderService.class);
        ProcessRouteSaveService routeSaveService = mock(ProcessRouteSaveService.class);
        ProcessRouteAppendService routeAppendService = mock(ProcessRouteAppendService.class);
        pricingBatchService = mock(ProcessStepPricingBatchService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new ProcessOrderController(processOrderService, routeSaveService, routeAppendService, pricingBatchService))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createOrder_withoutToken_returnsUnauthorized() throws Exception {
        mvc.perform(post("/api/process-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        verify(processOrderService, never()).create(any());
    }

    @Test
    void createOrder_withFinanceRole_returnsForbidden() throws Exception {
        authorizeAs("finance");

        mvc.perform(post("/api/process-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(processOrderService, never()).create(any());
    }

    @Test
    void createOrder_withoutOriginalRolls_returnsBadRequest() throws Exception {
        authorizeAs("operator");

        mvc.perform(post("/api/process-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerUuid\":\"customer-1\",\"orderDate\":\"2026-07-07\",\"originalRolls\":[]}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("originalRolls: 原纸明细不能为空"));

        verify(processOrderService, never()).create(any());
    }

    @Test
    void createOrder_withOperatorRole_bindsPayloadAndReturnsUuid() throws Exception {
        authorizeAs("operator");
        when(processOrderService.create(any())).thenReturn("order-uuid");

        mvc.perform(post("/api/process-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderPayload())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("order-uuid"));

        ArgumentCaptor<ProcessOrderCreateDTO> captor = ArgumentCaptor.forClass(ProcessOrderCreateDTO.class);
        verify(processOrderService).create(captor.capture());
        assertEquals("customer-1", captor.getValue().getCustomerUuid());
        assertEquals("测试纸", captor.getValue().getOriginalRolls().getFirst().getPaperName());
        assertEquals(new BigDecimal("1200.50"), captor.getValue().getOriginalRolls().getFirst().getRollWeight());
    }

    @Test
    void changeStatus_withOperatorRole_returnsForbidden() throws Exception {
        authorizeAs("operator");

        mvc.perform(put("/api/process-orders/order-uuid/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStatus\":3,\"reason\":\"转待回录\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(processOrderService, never()).changeStatus(any(), any(), any());
    }

    @Test
    void print_withAdminRoleAndEmptyBody_usesDefaultPrintDto() throws Exception {
        authorizeAs("admin");
        PrintResultVO result = new PrintResultVO();
        result.setOrderUuid("order-uuid");
        when(processOrderService.print(eq("order-uuid"), any())).thenReturn(result);

        mvc.perform(post("/api/process-orders/order-uuid/print")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderUuid").value("order-uuid"));

        ArgumentCaptor<PrintDTO> captor = ArgumentCaptor.forClass(PrintDTO.class);
        verify(processOrderService).print(eq("order-uuid"), captor.capture());
        assertNull(captor.getValue().getReason());
    }

    @Test
    void print_withOperatorRole_returnsForbidden() throws Exception {
        authorizeAs("operator");

        mvc.perform(post("/api/process-orders/order-uuid/print")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(processOrderService, never()).print(any(), any());
    }

    @Test
    void backRecord_withOperatorRole_bindsPayloadAndReturnsResult() throws Exception {
        authorizeAs("operator");
        BackRecordResultVO result = new BackRecordResultVO();
        result.setOrderUuid("order-uuid");
        when(processOrderService.backRecord(eq("order-uuid"), any())).thenReturn(result);

        mvc.perform(post("/api/process-orders/order-uuid/back-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(backRecordPayload())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderUuid").value("order-uuid"));

        ArgumentCaptor<BackRecordDTO> captor = ArgumentCaptor.forClass(BackRecordDTO.class);
        verify(processOrderService).backRecord(eq("order-uuid"), captor.capture());
        assertEquals("warehouse-1", captor.getValue().getWarehouseUuid());
        assertEquals(new BigDecimal("1198.00"), captor.getValue().getRolls().getFirst().getActualWeight());
    }

    @Test
    void pricingAdjustment_withOrderClerkRole_bindsPayloadAndReturnsFee() throws Exception {
        authorizeAs("order_clerk");
        FeeResultVO result = new FeeResultVO();
        result.setOrderUuid("order-uuid");
        when(processOrderService.adjustProcessStepPricing(eq("step-uuid"), any()))
                .thenReturn(result);

        mvc.perform(put("/api/process-orders/steps/step-uuid/pricing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"billingMode\":2,\"billingQuantity\":1,\"reason\":\"客户仅加工20米\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderUuid").value("order-uuid"));

        ArgumentCaptor<ProcessStepPricingAdjustmentDTO> captor =
                ArgumentCaptor.forClass(ProcessStepPricingAdjustmentDTO.class);
        verify(processOrderService).adjustProcessStepPricing(eq("step-uuid"), captor.capture());
        assertEquals(2, captor.getValue().getBillingMode());
        assertEquals(new BigDecimal("1"), captor.getValue().getBillingQuantity());
    }

    @Test
    void pricingAdjustment_withViewerRole_returnsForbidden() throws Exception {
        authorizeAs("viewer");

        mvc.perform(put("/api/process-orders/steps/step-uuid/pricing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"billingMode\":2,\"billingQuantity\":1,\"reason\":\"customer adjustment\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(processOrderService, never()).adjustProcessStepPricing(any(), any());
    }

    @Test
    void pricingBatchPreview_withOrderClerkRole_bindsPayload() throws Exception {
        authorizeAs("order_clerk");
        ProcessStepPricingBatchPreviewVO result = new ProcessStepPricingBatchPreviewVO();
        result.setOrderUuid("order-uuid");
        when(pricingBatchService.preview(eq("order-uuid"), any())).thenReturn(result);

        mvc.perform(post("/api/process-orders/order-uuid/pricing-adjustments/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pricingBatchPayload())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderUuid").value("order-uuid"));

        ArgumentCaptor<ProcessStepPricingBatchDTO> captor =
                ArgumentCaptor.forClass(ProcessStepPricingBatchDTO.class);
        verify(pricingBatchService).preview(eq("order-uuid"), captor.capture());
        assertEquals(6, captor.getValue().getExpectedOrderVersion());
        assertEquals(1, captor.getValue().getGroups().getFirst().getStepType());
        assertEquals(new BigDecimal("100"), captor.getValue().getGroups().getFirst().getBillingUnitPrice());
    }

    @Test
    void pricingBatchApply_withViewerRole_returnsForbidden() throws Exception {
        authorizeAs("viewer");

        mvc.perform(put("/api/process-orders/order-uuid/pricing-adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pricingBatchPayload())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(pricingBatchService, never()).apply(any(), any());
    }

    private void authorizeAs(String roleCode) {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("user-uuid")
                .username("tester")
                .realName("测试员")
                .roleCode(roleCode)
                .build());
    }

    private String orderPayload() {
        return """
                {"customerUuid":"customer-1","orderDate":"2026-07-07","originalRolls":[{"paperName":"测试纸","gramWeight":200,"originalWidth":1880,"rollWeight":1200.50}]}
                """;
    }

    private String backRecordPayload() {
        return """
                {"warehouseUuid":"warehouse-1","rolls":[{"uuid":"roll-1","actualWeight":1198.00}]}
                """;
    }

    private String pricingBatchPayload() {
        return """
                {"expectedOrderVersion":6,"reason":"customer agreement price","groups":[{"stepType":1,"stepUuids":["step-1"],"restoreStandard":false,"billingUnitPrice":100}]}
                """;
    }
}

package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.common.PageResult;
import com.paper.mes.delivery.controller.DeliveryInventoryController;
import com.paper.mes.delivery.dto.DeliveryInventorySummaryVO;
import com.paper.mes.delivery.service.DeliveryInventoryService;
import com.paper.mes.delivery.service.DeliveryInventoryOrderGroupPageService;
import com.paper.mes.delivery.service.AvailableFinishPageService;
import com.paper.mes.delivery.service.DeliveryInventoryWarehouseRepairService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeliveryInventoryControllerContractTest {

    private static final String TOKEN = "test-token";
    private AuthService authService;
    private DeliveryInventoryService inventoryService;
    private AvailableFinishPageService availableFinishPageService;
    private DeliveryInventoryOrderGroupPageService orderGroupPageService;
    private DeliveryInventoryWarehouseRepairService warehouseRepairService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        inventoryService = mock(DeliveryInventoryService.class);
        availableFinishPageService = mock(AvailableFinishPageService.class);
        orderGroupPageService = mock(DeliveryInventoryOrderGroupPageService.class);
        warehouseRepairService = mock(DeliveryInventoryWarehouseRepairService.class);
        mvc = MockMvcBuilders.standaloneSetup(new DeliveryInventoryController(
                        inventoryService, availableFinishPageService, orderGroupPageService, warehouseRepairService))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void customers_withViewerRole_isReadable() throws Exception {
        authorizeAs("viewer");
        when(inventoryService.pageCustomers(any())).thenReturn(new PageResult<>());

        mvc.perform(get("/api/delivery-orders/inventory/customers")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(inventoryService).pageCustomers(any());
    }

    @Test
    void summary_bindsSharedFilters() throws Exception {
        authorizeAs("viewer");
        when(inventoryService.summary(any())).thenReturn(new DeliveryInventorySummaryVO());

        mvc.perform(get("/api/delivery-orders/inventory/summary")
                        .param("keyword", "牛卡")
                        .param("stockState", "1")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk());

        verify(inventoryService).summary(any());
    }

    @Test
    void finishes_withoutCustomer_isReadableForGlobalView() throws Exception {
        authorizeAs("viewer");
        when(inventoryService.pageFinishes(any())).thenReturn(new PageResult<>());

        mvc.perform(get("/api/delivery-orders/inventory/finishes")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk());

        verify(inventoryService).pageFinishes(any());
    }

    @Test
    void finishes_withInvalidStockState_returnsBadRequest() throws Exception {
        authorizeAs("viewer");

        mvc.perform(get("/api/delivery-orders/inventory/finishes")
                        .param("stockState", "3")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(jsonPath("$.code").value(400));

        verify(inventoryService, never()).pageFinishes(any());
    }

    @Test
    void orderGroups_withViewerRole_isReadable() throws Exception {
        authorizeAs("viewer");
        when(orderGroupPageService.page(any())).thenReturn(new PageResult<>());

        mvc.perform(get("/api/delivery-orders/inventory/order-groups")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(orderGroupPageService).page(any());
    }

    @Test
    void validateAvailability_requiresManagePermission() throws Exception {
        authorizeAs("viewer");

        mvc.perform(post("/api/delivery-orders/inventory/validate-availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerUuid\":\"customer-1\",\"finishUuids\":[\"finish-1\"]}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden());

        verify(inventoryService, never()).validateAvailability(any());
    }

    @Test
    void availableFinishes_requiresManagePermission() throws Exception {
        authorizeAs("viewer");

        mvc.perform(get("/api/delivery-orders/inventory/available-finishes")
                        .param("customerUuid", "customer-1")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden());

        verify(availableFinishPageService, never()).page(any());
    }

    @Test
    void unassigned_withViewerRole_isForbidden() throws Exception {
        authorizeAs("viewer");

        mvc.perform(get("/api/delivery-orders/inventory/unassigned")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden());

        verify(warehouseRepairService, never()).page(any());
    }

    @Test
    void assignWarehouse_withoutReason_returnsBadRequest() throws Exception {
        authorizeAs("warehouse");

        mvc.perform(post("/api/delivery-orders/inventory/assign-warehouse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderUuids\":[\"order-1\"],\"warehouseUuid\":\"warehouse-1\",\"reason\":\"bad\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(warehouseRepairService, never()).assign(any());
    }

    @Test
    void legacySynchronousExportEndpoint_isRemoved() throws Exception {
        authorizeAs("viewer");

        mvc.perform(get("/api/delivery-orders/inventory/export")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNotFound());

        verify(inventoryService, never()).pageFinishes(any());
    }

    private void authorizeAs(String roleCode) {
        when(authService.resolveToken(any(HttpServletRequest.class))).thenReturn(TOKEN);
        when(authService.currentUser(TOKEN)).thenReturn(CurrentUser.builder()
                .uuid("user-uuid").username("tester").realName("tester").roleCode(roleCode).build());
    }
}

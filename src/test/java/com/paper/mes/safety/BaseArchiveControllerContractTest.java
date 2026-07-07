package com.paper.mes.safety;

import com.paper.mes.auth.config.AuthInterceptor;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.PermissionInterceptor;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.customer.controller.CustomerController;
import com.paper.mes.customer.dto.CustomerSaveDTO;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.machine.controller.MachineController;
import com.paper.mes.machine.dto.MachineSaveDTO;
import com.paper.mes.machine.service.MachineService;
import com.paper.mes.paper.controller.PaperController;
import com.paper.mes.paper.dto.PaperSaveDTO;
import com.paper.mes.paper.service.PaperService;
import com.paper.mes.warehouse.controller.WarehouseController;
import com.paper.mes.warehouse.dto.WarehouseSaveDTO;
import com.paper.mes.warehouse.service.WarehouseService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BaseArchiveControllerContractTest {

    private static final String TOKEN = "test-token";

    private AuthService authService;
    private CustomerService customerService;
    private PaperService paperService;
    private MachineService machineService;
    private WarehouseService warehouseService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        customerService = mock(CustomerService.class);
        paperService = mock(PaperService.class);
        machineService = mock(MachineService.class);
        warehouseService = mock(WarehouseService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new CustomerController(customerService),
                        new PaperController(paperService),
                        new MachineController(machineService),
                        new WarehouseController(warehouseService))
                .addInterceptors(new AuthInterceptor(authService),
                        new PermissionInterceptor(new PermissionChecker()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createCustomer_withoutToken_returnsUnauthorized() throws Exception {
        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        verify(customerService, never()).create(any());
    }

    @Test
    void createCustomer_withOperatorRole_returnsForbidden() throws Exception {
        authorizeAs("operator");

        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerPayload())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        verify(customerService, never()).create(any());
    }

    @Test
    void createCustomer_withBlankName_returnsBadRequest() throws Exception {
        authorizeAs("admin");

        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("customerName: 客户名称不能为空"));

        verify(customerService, never()).create(any());
    }

    @Test
    void createCustomer_withAdminRole_bindsPayloadAndReturnsUuid() throws Exception {
        authorizeAs("admin");
        when(customerService.create(any())).thenReturn("customer-uuid");

        mvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerPayload())
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("customer-uuid"));

        ArgumentCaptor<CustomerSaveDTO> captor = ArgumentCaptor.forClass(CustomerSaveDTO.class);
        verify(customerService).create(captor.capture());
        assertEquals("测试客户", captor.getValue().getCustomerName());
        assertEquals(new BigDecimal("12.50"), captor.getValue().getSawPrice());
    }

    @Test
    void createPaper_withAdminRole_bindsPayloadAndReturnsUuid() throws Exception {
        authorizeAs("admin");
        when(paperService.create(any())).thenReturn("paper-uuid");

        mvc.perform(post("/api/papers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paperName\":\"测试纸\",\"gramWeight\":200,\"paperType\":\"牛卡\"}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("paper-uuid"));

        ArgumentCaptor<PaperSaveDTO> captor = ArgumentCaptor.forClass(PaperSaveDTO.class);
        verify(paperService).create(captor.capture());
        assertEquals("测试纸", captor.getValue().getPaperName());
        assertEquals(200, captor.getValue().getGramWeight());
    }

    @Test
    void createMachine_withAdminRole_bindsPayloadAndReturnsUuid() throws Exception {
        authorizeAs("admin");
        when(machineService.create(any())).thenReturn("machine-uuid");

        mvc.perform(post("/api/machines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"machineName\":\"一号复卷机\",\"machineType\":2,\"status\":1}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("machine-uuid"));

        ArgumentCaptor<MachineSaveDTO> captor = ArgumentCaptor.forClass(MachineSaveDTO.class);
        verify(machineService).create(captor.capture());
        assertEquals("一号复卷机", captor.getValue().getMachineName());
        assertEquals(2, captor.getValue().getMachineType());
    }

    @Test
    void createWarehouse_withAdminRole_bindsPayloadAndReturnsUuid() throws Exception {
        authorizeAs("admin");
        when(warehouseService.create(any())).thenReturn("warehouse-uuid");

        mvc.perform(post("/api/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseName\":\"成品仓\",\"location\":\"一楼\",\"status\":1}")
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("warehouse-uuid"));

        ArgumentCaptor<WarehouseSaveDTO> captor = ArgumentCaptor.forClass(WarehouseSaveDTO.class);
        verify(warehouseService).create(captor.capture());
        assertEquals("成品仓", captor.getValue().getWarehouseName());
        assertEquals("一楼", captor.getValue().getLocation());
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

    private String customerPayload() {
        return """
                {"customerName":"测试客户","contact":"张三","settleType":2,"sawPrice":12.50}
                """;
    }
}

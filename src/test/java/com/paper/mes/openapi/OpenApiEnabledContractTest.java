package com.paper.mes.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.machine.service.MachineService;
import com.paper.mes.paper.service.PaperService;
import com.paper.mes.warehouse.service.WarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = OpenApiContractTestApplication.class,
        properties = "management.endpoint.health.validate-group-membership=false"
)
@AutoConfigureMockMvc
@ActiveProfiles({"test", "openapi-contract"})
class OpenApiEnabledContractTest {

    private static final Path OUTPUT = Path.of("target", "openapi", "paper-mes.json");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private PaperService paperService;

    @MockitoBean
    private MachineService machineService;

    @MockitoBean
    private WarehouseService warehouseService;

    @Test
    void schema_contains_stable_master_data_read_operation_ids() throws Exception {
        String content = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode schema = objectMapper.readTree(content);
        assertOperationId(schema, "/api/customers", "listCustomers");
        assertOperationId(schema, "/api/customers/{uuid}", "getCustomer");
        assertThat(schema.at("/components/schemas/CustomerVO").isObject()).isTrue();
        assertOperationId(schema, "/api/papers", "listPapers");
        assertOperationId(schema, "/api/papers/{uuid}", "getPaper");
        assertThat(schema.at("/components/schemas/Paper").isObject()).isTrue();
        assertOperationId(schema, "/api/machines", "listMachines");
        assertOperationId(schema, "/api/machines/{uuid}", "getMachine");
        assertThat(schema.at("/components/schemas/MachineVO").isObject()).isTrue();
        assertOperationId(schema, "/api/warehouses", "listWarehouses");
        assertOperationId(schema, "/api/warehouses/{uuid}", "getWarehouse");
        assertThat(schema.at("/components/schemas/Warehouse").isObject()).isTrue();
        writeSchema(content);
    }

    private void assertOperationId(JsonNode schema, String path, String expected) {
        assertThat(schema.path("paths").path(path).path("get").path("operationId").asText())
                .isEqualTo(expected);
    }

    private void writeSchema(String content) throws Exception {
        Files.createDirectories(OUTPUT.getParent());
        Files.writeString(OUTPUT, content);
    }
}

package com.paper.mes.openapi;

import com.paper.mes.customer.service.CustomerService;
import com.paper.mes.paper.service.PaperService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = OpenApiContractTestApplication.class,
        properties = "management.endpoint.health.validate-group-membership=false"
)
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class OpenApiProductionContractTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private PaperService paperService;

    @Test
    void production_does_not_expose_api_schema() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isNotFound());
    }

    @Test
    void production_does_not_expose_swagger_ui() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isNotFound());
    }
}

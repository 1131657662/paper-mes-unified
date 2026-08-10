package com.paper.mes.observability;

import com.paper.mes.common.GlobalExceptionHandler;
import com.paper.mes.observability.controller.RumMetricController;
import com.paper.mes.observability.service.RumService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RumMetricControllerContractTest {

    private final RumService rumService = mock(RumService.class);
    private final LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new RumMetricController(rumService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void collect_acceptsAllowlistedAnonymousMetric() throws Exception {
        mvc.perform(post("/api/rum")
                        .contentType(APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isNoContent());

        verify(rumService).record(any(), eq("127.0.0.1"));
    }

    @Test
    void collect_rejectsUnknownBusinessFields() throws Exception {
        mvc.perform(post("/api/rum")
                        .contentType(APPLICATION_JSON)
                        .content(validPayload().replace("}", ",\"username\":\"forbidden\"}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void collect_rejectsQueryBearingRoute() throws Exception {
        mvc.perform(post("/api/rum")
                        .contentType(APPLICATION_JSON)
                        .content(validPayload().replace("/dashboard", "/dashboard?order=secret")))
                .andExpect(status().isBadRequest());
    }

    private String validPayload() {
        return "{" +
                "\"name\":\"LCP\",\"value\":842.5,\"rating\":\"good\"," +
                "\"route\":\"/dashboard\",\"browser\":\"chrome\",\"browserVersion\":\"136\"," +
                "\"deviceTier\":\"mid\",\"networkType\":\"4g\"}";
    }
}

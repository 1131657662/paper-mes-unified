package com.paper.mes.openapi;

import com.paper.mes.config.OpenApiConfig;
import com.paper.mes.customer.controller.CustomerController;
import com.paper.mes.paper.controller.PaperController;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@Import({CustomerController.class, PaperController.class, OpenApiConfig.class})
class OpenApiContractTestApplication {
}

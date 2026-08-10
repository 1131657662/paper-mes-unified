package com.paper.mes.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Paper MES API",
        version = "v1",
        description = "Paper MES backend contract. Runtime exposure is profile-controlled."
))
public class OpenApiConfig {
}

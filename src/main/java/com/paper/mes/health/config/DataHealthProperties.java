package com.paper.mes.health.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "app.data-health")
public class DataHealthProperties {

    @Min(1)
    private int backRecordOverdueDays = 3;

    @Min(1)
    private int receivableOverdueDays = 30;
}

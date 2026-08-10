package com.paper.mes.observability.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** Runtime controls for the first-party, anonymous Web Vitals collector. */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.rum")
public class RumProperties {

    private boolean enabled;

    @Min(1)
    @Max(10_000)
    private int maxEventsPerMinute = 300;

    @Min(100)
    @Max(100_000)
    private int maxTrackedClients = 10_000;
}

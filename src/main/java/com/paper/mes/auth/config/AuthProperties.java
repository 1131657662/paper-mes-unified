package com.paper.mes.auth.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    @Min(1)
    private int sessionHours = 12;
    @Min(1)
    private int sessionRetentionDays = 7;
    @Min(1)
    private int loginMaxFailures = 5;
    @Min(1)
    private int loginWindowSeconds = 300;
    @Min(1)
    private int loginLockSeconds = 900;
    private boolean cookieSecure;
}

package com.paper.mes.remain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Release switch for the unfinished remain business module. */
@Data
@Component
@ConfigurationProperties(prefix = "app.remain")
public class RemainProperties {

    /** Keep the module unavailable until its business workflow is released. */
    private boolean enabled = false;
}

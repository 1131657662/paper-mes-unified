package com.paper.mes.exporttask.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.export-task.executor")
public class ExportTaskExecutorProperties {
    @Min(1)
    @Max(16)
    private int workerCount = 2;
    @Min(1)
    @Max(1000)
    private int queueCapacity = 50;
    @Min(1)
    @Max(60)
    private int shutdownWaitSeconds = 5;
}

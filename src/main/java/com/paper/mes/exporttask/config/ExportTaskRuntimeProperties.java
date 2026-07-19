package com.paper.mes.exporttask.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.export-task")
public class ExportTaskRuntimeProperties {
    @Min(1)
    @Max(1440)
    private long staleMinutes = 10;
    @Min(1)
    @Max(300)
    private int heartbeatIntervalSeconds = 30;
    @Min(1)
    @Max(10080)
    private long orphanRetentionMinutes = 60;
    @Min(1)
    @Max(1000)
    private int orphanScanLimit = 100;
    @Min(0)
    private long storageMinFreeBytes = 0;
    @Min(0)
    @Max(100)
    private double storageMinFreePercent = 5;
    @Min(5000)
    @Max(86400000)
    private long storageAlertInitialDelayMs = 30000;
    @Min(5000)
    @Max(86400000)
    private long storageAlertCheckDelayMs = 60000;

    @AssertTrue(message = "导出任务心跳间隔必须不超过停滞窗口的三分之一")
    public boolean isHeartbeatIntervalSafe() {
        return heartbeatIntervalSeconds * 3L <= staleMinutes * 60L;
    }

    @AssertTrue(message = "导出孤儿文件保留窗口不能小于停滞恢复窗口")
    public boolean isOrphanRetentionSafe() {
        return orphanRetentionMinutes >= staleMinutes;
    }
}

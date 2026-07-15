package com.paper.mes.backup.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "app.backup")
public class BackupProperties {

    private boolean enabled;
    private String rootDir;
    private String backupScript;
    private String verifyScript;
    private String envFile;
    private String sourceDbName = "paper_processing";
    private Duration commandTimeout = Duration.ofMinutes(30);
}

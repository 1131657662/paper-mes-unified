package com.paper.mes.backup.service;

import com.paper.mes.backup.config.BackupProperties;
import com.paper.mes.config.FileStorageProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackupProcessEnvironmentTest {

    @Test
    void variables_withMysqlUrl_extractsConnectionSettings() {
        DataSourceProperties dataSource = new DataSourceProperties();
        dataSource.setUrl("jdbc:mysql://db.internal:3307/paper_processing?useSSL=false");
        dataSource.setUsername("backup_user");
        dataSource.setPassword("secret");
        BackupProperties backup = new BackupProperties();
        backup.setSourceDbName("paper_processing");

        Map<String, String> variables = new BackupProcessEnvironment(
                dataSource, new FileStorageProperties(), backup).variables();

        assertEquals("db.internal", variables.get("DB_HOST"));
        assertEquals("3307", variables.get("DB_PORT"));
        assertEquals("paper_processing", variables.get("DB_NAME"));
        assertEquals("backup_user", variables.get("DB_USER"));
    }
}

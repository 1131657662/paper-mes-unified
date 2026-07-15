package com.paper.mes.backup.service;

import com.paper.mes.backup.config.BackupProperties;
import com.paper.mes.config.FileStorageProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BackupProcessEnvironment {

    private static final Pattern MYSQL_URL = Pattern.compile(
            "^jdbc:mysql://([^:/?]+)(?::(\\d+))?/([^?]+).*$");

    private final DataSourceProperties dataSource;
    private final FileStorageProperties storage;
    private final BackupProperties backup;

    public BackupProcessEnvironment(DataSourceProperties dataSource, FileStorageProperties storage,
                                    BackupProperties backup) {
        this.dataSource = dataSource;
        this.storage = storage;
        this.backup = backup;
    }

    public Map<String, String> variables() {
        Matcher matcher = MYSQL_URL.matcher(dataSource.getUrl());
        if (!matcher.matches()) {
            throw new IllegalStateException("仅支持自动解析 MySQL 数据源地址");
        }
        Map<String, String> variables = new HashMap<>();
        variables.put("DB_HOST", matcher.group(1));
        variables.put("DB_PORT", matcher.group(2) == null ? "3306" : matcher.group(2));
        variables.put("DB_NAME", backup.getSourceDbName());
        variables.put("SOURCE_DB_NAME", backup.getSourceDbName());
        variables.put("DB_USER", dataSource.getUsername());
        variables.put("DB_PASSWORD", dataSource.getPassword());
        variables.put("DB_ADMIN_USER", dataSource.getUsername());
        variables.put("DB_ADMIN_PASSWORD", dataSource.getPassword());
        variables.put("UPLOAD_DIR", Path.of(storage.getDir()).toAbsolutePath().normalize().toString());
        return variables;
    }
}

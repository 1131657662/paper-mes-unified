package com.paper.mes.backup.service;

import java.nio.file.Path;
import java.util.List;

public record BackupRuntime(
        String platform,
        String runner,
        Path root,
        Path backupScript,
        Path verifyScript,
        Path envFile,
        List<String> missingComponents) {

    public boolean configured() {
        return missingComponents.isEmpty();
    }
}

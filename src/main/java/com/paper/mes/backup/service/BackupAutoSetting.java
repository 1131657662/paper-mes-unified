package com.paper.mes.backup.service;

import java.time.LocalTime;

public record BackupAutoSetting(boolean enabled, LocalTime executionTime) {
}

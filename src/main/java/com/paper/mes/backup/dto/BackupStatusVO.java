package com.paper.mes.backup.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BackupStatusVO {

    private boolean enabled;
    private boolean configured;
    private boolean running;
    private String platform;
    private String runner;
    private java.util.List<String> missingComponents;
    private String runningOperation;
    private LocalDateTime latestBackupAt;
    private LocalDateTime latestVerifiedAt;
    private Long totalSpaceBytes;
    private Long usableSpaceBytes;
    private Integer retentionDays;
    private Integer backupCount;
    private Long latestBackupAgeHours;
    private Boolean automaticEnabled;
    private String automaticExecutionTime;
    private LocalDateTime lastAutomaticAt;
    private String lastAutomaticStatus;
    private Long automaticConsecutiveFailures;
    private LocalDateTime nextAutomaticAt;
    private String offsiteStatus;
    private LocalDateTime offsiteLastSyncAt;
    private String offsiteRemoteName;
    private String message;
}

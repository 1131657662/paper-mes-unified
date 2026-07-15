package com.paper.mes.backup.service;

import com.paper.mes.backup.dto.BackupOperationVO;
import com.paper.mes.backup.dto.BackupRecordVO;
import com.paper.mes.backup.dto.BackupStatusVO;
import com.paper.mes.backup.dto.BackupTaskVO;

import java.util.List;

public interface BackupService {

    List<BackupRecordVO> list();

    BackupStatusVO status();

    BackupStatusVO updateEnabled(boolean enabled);

    BackupStatusVO updateAutomatic(boolean enabled, String executionTime);

    BackupOperationVO startBackup();

    BackupOperationVO startVerification(String backupId);

    List<BackupTaskVO> recentTasks();

    BackupStatusVO updateRetention(int retentionDays);

    BackupOperationVO cleanupExpired();

    void delete(String backupId);
}

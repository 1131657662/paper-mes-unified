package com.paper.mes.backup.service.impl;

import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.backup.dto.BackupOperationVO;
import com.paper.mes.backup.dto.BackupRecordVO;
import com.paper.mes.backup.dto.BackupStatusVO;
import com.paper.mes.backup.dto.BackupTaskVO;
import com.paper.mes.backup.service.BackupCatalog;
import com.paper.mes.backup.service.BackupAutoSettingService;
import com.paper.mes.backup.service.BackupAutomaticCoordinator;
import com.paper.mes.backup.service.BackupAutomaticStatus;
import com.paper.mes.backup.service.BackupFeatureSettingService;
import com.paper.mes.backup.service.BackupHealth;
import com.paper.mes.backup.service.BackupMaintenanceService;
import com.paper.mes.backup.service.BackupService;
import com.paper.mes.backup.service.BackupRuntime;
import com.paper.mes.backup.service.BackupRuntimeResolver;
import com.paper.mes.backup.service.BackupTaskExecutor;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BackupServiceImpl implements BackupService {

    private final BackupCatalog catalog;
    private final BackupFeatureSettingService featureSettingService;
    private final BackupRuntimeResolver runtimeResolver;
    private final BackupTaskExecutor taskExecutor;
    private final BackupMaintenanceService maintenanceService;
    private final BackupAutoSettingService autoSettingService;
    private final BackupAutomaticCoordinator automaticCoordinator;

    public BackupServiceImpl(BackupCatalog catalog, BackupFeatureSettingService featureSettingService,
                             BackupRuntimeResolver runtimeResolver, BackupTaskExecutor taskExecutor,
                             BackupMaintenanceService maintenanceService,
                             BackupAutoSettingService autoSettingService,
                             BackupAutomaticCoordinator automaticCoordinator) {
        this.catalog = catalog;
        this.featureSettingService = featureSettingService;
        this.runtimeResolver = runtimeResolver;
        this.taskExecutor = taskExecutor;
        this.maintenanceService = maintenanceService;
        this.autoSettingService = autoSettingService;
        this.automaticCoordinator = automaticCoordinator;
    }

    @Override
    public List<BackupRecordVO> list() {
        return catalog.list();
    }

    @Override
    public BackupStatusVO status() {
        List<BackupRecordVO> records = catalog.list();
        LocalDateTime latestVerified = records.stream()
                .map(BackupRecordVO::getVerifiedAt).filter(value -> value != null)
                .max(LocalDateTime::compareTo).orElse(null);
        BackupRuntime runtime = runtimeResolver.resolve();
        BackupHealth health = maintenanceService.health();
        BackupAutomaticStatus automatic = automaticCoordinator.status();
        LocalDateTime latestBackup = records.isEmpty() ? null : records.getFirst().getCreatedAt();
        return BackupStatusVO.builder()
                .enabled(featureSettingService.isEnabled())
                .configured(runtime.configured())
                .running(taskExecutor.isRunning())
                .platform(runtime.platform())
                .runner(runtime.runner())
                .missingComponents(runtime.missingComponents())
                .runningOperation(taskExecutor.runningOperation())
                .latestBackupAt(latestBackup)
                .latestVerifiedAt(latestVerified)
                .totalSpaceBytes(health.totalSpaceBytes())
                .usableSpaceBytes(health.usableSpaceBytes())
                .retentionDays(health.retentionDays())
                .backupCount(records.size())
                .latestBackupAgeHours(latestBackup == null ? null : Duration.between(latestBackup, LocalDateTime.now()).toHours())
                .automaticEnabled(automatic.enabled())
                .automaticExecutionTime(automatic.executionTime().toString())
                .lastAutomaticAt(automatic.lastStartedAt())
                .lastAutomaticStatus(automatic.lastStatus())
                .automaticConsecutiveFailures(automatic.consecutiveFailures())
                .nextAutomaticAt(automatic.nextExecutionAt())
                .offsiteStatus(health.offsite().state().name())
                .offsiteLastSyncAt(health.offsite().lastSyncAt())
                .offsiteRemoteName(health.offsite().remoteName())
                .message(taskExecutor.latestMessage())
                .build();
    }

    @Override
    public BackupStatusVO updateEnabled(boolean enabled) {
        if (taskExecutor.isRunning()) {
            throw new BusinessException(ResultCode.CONFLICT, "备份任务执行中，暂时不能修改开关");
        }
        featureSettingService.updateEnabled(enabled);
        taskExecutor.setLatestMessage(enabled ? "管理端备份已启用" : "管理端备份已停用");
        return status();
    }

    @Override
    public BackupStatusVO updateAutomatic(boolean enabled, String executionTime) {
        requireIdle();
        autoSettingService.update(enabled, executionTime);
        taskExecutor.setLatestMessage(enabled ? "自动备份已启用" : "自动备份已停用");
        return status();
    }

    @Override
    public BackupOperationVO startBackup() {
        requireAvailable();
        String operator = AuthContextHolder.currentDisplayName();
        taskExecutor.startBackup(operator);
        return new BackupOperationVO(true, "备份任务已开始");
    }

    @Override
    public BackupOperationVO startVerification(String backupId) {
        requireAvailable();
        Path backupDirectory = catalog.requireBackup(backupId);
        String operator = AuthContextHolder.currentDisplayName();
        taskExecutor.startVerification(backupId, backupDirectory, operator);
        return new BackupOperationVO(true, "恢复演练已开始");
    }

    @Override
    public List<BackupTaskVO> recentTasks() {
        return taskExecutor.recentTasks();
    }

    @Override
    public BackupStatusVO updateRetention(int retentionDays) {
        requireIdle();
        maintenanceService.updateRetention(retentionDays);
        return status();
    }

    @Override
    public BackupOperationVO cleanupExpired() {
        requireIdle();
        int deleted = maintenanceService.cleanupExpired(AuthContextHolder.currentDisplayName());
        return new BackupOperationVO(true, "清理完成，共删除 " + deleted + " 份过期备份");
    }

    @Override
    public void delete(String backupId) {
        requireIdle();
        maintenanceService.delete(backupId, AuthContextHolder.currentDisplayName());
    }

    private void requireAvailable() {
        if (!featureSettingService.isEnabled()) {
            throw new BusinessException(ResultCode.CONFLICT, "管理端备份功能未启用");
        }
        if (!isConfigured()) {
            throw new BusinessException(ResultCode.CONFLICT, "备份脚本配置不完整");
        }
    }

    private boolean isConfigured() {
        return runtimeResolver.resolve().configured();
    }

    private void requireIdle() {
        if (taskExecutor.isRunning()) {
            throw new BusinessException(ResultCode.CONFLICT, "备份任务执行中，暂时不能清理或修改策略");
        }
    }
}

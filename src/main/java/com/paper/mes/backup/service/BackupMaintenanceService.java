package com.paper.mes.backup.service;

import com.paper.mes.backup.dto.BackupRecordVO;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.oplog.service.OperationLogService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class BackupMaintenanceService {

    private final BackupCatalog catalog;
    private final BackupRetentionSettingService retentionSetting;
    private final OperationLogService operationLogService;
    private final BackupOperationGuard operationGuard;
    private final OffsiteBackupStatusReader offsiteStatusReader;

    public BackupMaintenanceService(BackupCatalog catalog, BackupRetentionSettingService retentionSetting,
                                    OperationLogService operationLogService,
                                    BackupOperationGuard operationGuard,
                                    OffsiteBackupStatusReader offsiteStatusReader) {
        this.catalog = catalog;
        this.retentionSetting = retentionSetting;
        this.operationLogService = operationLogService;
        this.operationGuard = operationGuard;
        this.offsiteStatusReader = offsiteStatusReader;
    }

    public BackupHealth health() {
        try {
            Files.createDirectories(catalog.root());
            FileStore store = Files.getFileStore(catalog.root());
            return new BackupHealth(store.getTotalSpace(), store.getUsableSpace(),
                    retentionSetting.retentionDays(), offsiteStatusReader.read());
        } catch (IOException ex) {
            throw new IllegalStateException("无法读取备份磁盘状态", ex);
        }
    }

    public void updateRetention(int days) {
        retentionSetting.update(days);
    }

    public void delete(String backupId, String operator) {
        acquire("DELETE");
        try {
            deleteBackup(backupId, operator);
        } finally {
            operationGuard.release("DELETE");
        }
    }

    public int cleanupExpired(String operator) {
        acquire("CLEANUP");
        try {
            return cleanup(LocalDateTime.now().minusDays(retentionSetting.retentionDays()), operator);
        } finally {
            operationGuard.release("CLEANUP");
        }
    }

    private void deleteBackup(String backupId, String operator) {
        List<BackupRecordVO> records = catalog.list();
        BackupRecordVO target = records.stream().filter(record -> record.getId().equals(backupId))
                .findFirst().orElseThrow(() -> new BusinessException("备份记录不存在"));
        long verifiedCount = records.stream().filter(this::isVerified).count();
        long completeCount = records.stream().filter(this::isComplete).count();
        if (isVerified(target) && verifiedCount <= 1) {
            throw new BusinessException("至少需要保留一份恢复验证通过的备份，当前备份不能删除");
        }
        if (verifiedCount == 0 && isComplete(target) && completeCount <= 1) {
            throw new BusinessException("至少需要保留一份有效备份，当前备份不能删除");
        }
        deleteDirectory(catalog.requireBackup(backupId));
        operationLogService.record(OperationLogService.BIZ_TYPE_BACKUP, backupId, backupId,
                OperationLogService.ACTION_BACKUP_DELETE, operator, "管理员手动删除本地备份");
    }

    private void acquire(String operation) {
        if (!operationGuard.acquire(operation)) {
            throw new BusinessException(ResultCode.CONFLICT, "已有备份任务或清理操作正在执行");
        }
    }

    private int cleanup(LocalDateTime cutoff, String operator) {
        List<BackupRecordVO> records = catalog.list();
        String protectedId = protectedBackupId(records);
        List<BackupRecordVO> expired = records.stream()
                .filter(record -> !record.getId().equals(protectedId))
                .filter(record -> record.getCreatedAt().isBefore(cutoff)).toList();
        expired.forEach(record -> deleteDirectory(catalog.requireBackup(record.getId())));
        if (!expired.isEmpty()) {
            operationLogService.record(OperationLogService.BIZ_TYPE_BACKUP, "retention-cleanup", null,
                    OperationLogService.ACTION_BACKUP_CLEANUP, operator,
                    "按保留策略清理 " + expired.size() + " 份备份");
        }
        return expired.size();
    }

    private void deleteDirectory(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(this::deletePath);
        } catch (IOException ex) {
            throw new IllegalStateException("备份删除失败", ex);
        }
    }

    private String protectedBackupId(List<BackupRecordVO> records) {
        return records.stream().filter(this::isVerified).findFirst()
                .or(() -> records.stream().filter(this::isComplete).findFirst())
                .map(BackupRecordVO::getId).orElse(null);
    }

    private boolean isComplete(BackupRecordVO record) {
        return record.isDatabaseArchive() && record.isChecksumAvailable();
    }

    private boolean isVerified(BackupRecordVO record) {
        return isComplete(record) && "VERIFIED".equals(record.getVerificationStatus());
    }

    private void deletePath(Path path) {
        try {
            Files.delete(path);
        } catch (IOException ex) {
            throw new IllegalStateException("备份文件删除失败", ex);
        }
    }
}

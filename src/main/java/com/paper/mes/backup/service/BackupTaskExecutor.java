package com.paper.mes.backup.service;

import com.paper.mes.backup.dto.BackupRecordVO;
import com.paper.mes.backup.dto.BackupTaskVO;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.oplog.service.OperationLogService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
public class BackupTaskExecutor {

    private final BackupCommandRunner runner;
    private final BackupCatalog catalog;
    private final BackupTaskHistoryService historyService;
    private final OperationLogService operationLogService;
    private final BackupOperationGuard operationGuard;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile String runningOperation;
    private volatile String latestMessage = "尚未执行管理端备份任务";

    public BackupTaskExecutor(BackupCommandRunner runner, BackupCatalog catalog,
                              BackupTaskHistoryService historyService,
                              OperationLogService operationLogService,
                              BackupOperationGuard operationGuard) {
        this.runner = runner;
        this.catalog = catalog;
        this.historyService = historyService;
        this.operationLogService = operationLogService;
        this.operationGuard = operationGuard;
    }

    public void startBackup(String operator) {
        submit("BACKUP", null, operator, () -> runner.backup(catalog.root()));
        operationLogService.record(OperationLogService.BIZ_TYPE_BACKUP, "manual-backup", null,
                OperationLogService.ACTION_BACKUP, operator, "手动发起数据备份");
    }

    public void startAutomaticBackup() {
        submit("AUTO_BACKUP", null, "system", () -> runner.backup(catalog.root()));
        operationLogService.record(OperationLogService.BIZ_TYPE_BACKUP, "automatic-backup", null,
                OperationLogService.ACTION_BACKUP, "system", "定时发起数据备份");
    }

    public void startVerification(String backupId, Path backupDirectory, String operator) {
        submit("VERIFY", backupId, operator, () -> runner.verify(catalog.root(), backupDirectory));
        operationLogService.record(OperationLogService.BIZ_TYPE_BACKUP, backupId, backupId,
                OperationLogService.ACTION_BACKUP_VERIFY, operator, "发起隔离恢复演练");
    }

    public boolean isRunning() {
        return runningOperation != null;
    }

    public String runningOperation() {
        return runningOperation;
    }

    public String latestMessage() {
        return latestMessage;
    }

    public List<BackupTaskVO> recentTasks() {
        return historyService.recent();
    }

    public void setLatestMessage(String message) {
        latestMessage = message;
    }

    private void submit(String operation, String backupId, String operator, Runnable task) {
        if (!operationGuard.acquire(operation)) {
            throw new BusinessException(ResultCode.CONFLICT, "已有备份任务正在执行");
        }
        runningOperation = operation;
        latestMessage = "任务执行中";
        try {
            String taskUuid = historyService.start(operation, backupId, operator);
            executor.submit(() -> runTask(new TaskContext(taskUuid, operation, backupId, task)));
        } catch (RuntimeException ex) {
            runningOperation = null;
            operationGuard.release(operation);
            throw ex;
        }
    }

    private void runTask(TaskContext context) {
        try {
            context.task().run();
            String resolvedBackupId = resolveBackupId(context);
            latestMessage = successMessage(context.operation());
            historyService.finish(context.taskUuid(), resolvedBackupId, true, latestMessage);
        } catch (RuntimeException ex) {
            latestMessage = failureMessage(ex);
            historyService.finish(context.taskUuid(), context.backupId(), false, latestMessage);
            log.error("Backup operation failed: {}", context.operation(), ex);
        } finally {
            runningOperation = null;
            operationGuard.release(context.operation());
        }
    }

    private String resolveBackupId(TaskContext context) {
        if (context.backupId() != null) return context.backupId();
        return catalog.list().stream().findFirst().map(BackupRecordVO::getId).orElse(null);
    }

    private String successMessage(String operation) {
        if ("VERIFY".equals(operation)) return "恢复演练通过";
        return "AUTO_BACKUP".equals(operation) ? "自动备份完成" : "备份完成";
    }

    private String failureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("超时")) {
            return "任务执行超时，请检查备份脚本和数据库连接";
        }
        if (message != null && message.contains("无法启动")) {
            return "备份脚本无法启动，请检查脚本路径和运行权限";
        }
        if (message != null && message.contains("退出码")) {
            return "备份脚本执行失败，请查看任务日志";
        }
        return "任务失败，请查看服务器日志";
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private record TaskContext(String taskUuid, String operation, String backupId, Runnable task) {
    }
}

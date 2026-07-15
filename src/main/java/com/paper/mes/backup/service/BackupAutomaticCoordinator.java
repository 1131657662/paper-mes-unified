package com.paper.mes.backup.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BackupAutomaticCoordinator {

    private final BackupAutomaticPolicy policy;
    private final BackupTaskExecutor taskExecutor;

    public BackupAutomaticCoordinator(BackupAutomaticPolicy policy,
                                      BackupTaskExecutor taskExecutor) {
        this.policy = policy;
        this.taskExecutor = taskExecutor;
    }

    public void runDueBackup() {
        LocalDateTime now = LocalDateTime.now();
        if (!policy.isDue(now) || taskExecutor.isRunning()) return;
        try {
            taskExecutor.startAutomaticBackup();
        } catch (BusinessException ex) {
            if (ex.getCode() != ResultCode.CONFLICT) throw ex;
        }
    }

    public BackupAutomaticStatus status() {
        return policy.status(LocalDateTime.now());
    }
}

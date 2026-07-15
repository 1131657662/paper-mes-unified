package com.paper.mes.backup.controller;

import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.auth.permission.RequirePermission;
import com.paper.mes.backup.dto.BackupOperationVO;
import com.paper.mes.backup.dto.BackupAutoSettingDTO;
import com.paper.mes.backup.dto.BackupEnabledDTO;
import com.paper.mes.backup.dto.BackupRecordVO;
import com.paper.mes.backup.dto.BackupRetentionDTO;
import com.paper.mes.backup.dto.BackupStatusVO;
import com.paper.mes.backup.dto.BackupTaskVO;
import com.paper.mes.backup.service.BackupService;
import com.paper.mes.common.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/backups")
@RequirePermission(Permissions.DATA_BACKUP)
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @GetMapping
    public R<List<BackupRecordVO>> list() {
        return R.success(backupService.list());
    }

    @GetMapping("/status")
    public R<BackupStatusVO> status() {
        return R.success(backupService.status());
    }

    @PutMapping("/enabled")
    public R<BackupStatusVO> updateEnabled(@Valid @RequestBody BackupEnabledDTO dto) {
        return R.success(backupService.updateEnabled(dto.getEnabled()));
    }

    @PutMapping("/automatic")
    public R<BackupStatusVO> updateAutomatic(@Valid @RequestBody BackupAutoSettingDTO dto) {
        return R.success(backupService.updateAutomatic(dto.getEnabled(), dto.getExecutionTime()));
    }

    @GetMapping("/tasks")
    public R<List<BackupTaskVO>> tasks() {
        return R.success(backupService.recentTasks());
    }

    @PutMapping("/retention")
    public R<BackupStatusVO> updateRetention(@Valid @RequestBody BackupRetentionDTO dto) {
        return R.success(backupService.updateRetention(dto.getRetentionDays()));
    }

    @PostMapping("/cleanup")
    public R<BackupOperationVO> cleanupExpired() {
        return R.success(backupService.cleanupExpired());
    }

    @PostMapping
    public R<BackupOperationVO> backup() {
        return R.success(backupService.startBackup());
    }

    @PostMapping("/{backupId}/verify")
    public R<BackupOperationVO> verify(@PathVariable String backupId) {
        return R.success(backupService.startVerification(backupId));
    }

    @DeleteMapping("/{backupId}")
    public R<Void> delete(@PathVariable String backupId) {
        backupService.delete(backupId);
        return R.success();
    }
}

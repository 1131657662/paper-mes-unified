package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OffsiteBackupSyncContractTest {

    @Test
    void syncScript_copiesByChecksumAndWritesAtomicSafeStatus() throws Exception {
        String script = source("deploy/sync-backups-rclone.example.sh");

        assertThat(script).contains("rclone copy", "--checksum", "--include '/????????-??????/**'");
        assertThat(script).contains(".remote-sync-status", "status=%s", "mv -f --");
        assertThat(script).doesNotContain("rclone sync", "RCLONE_PATH=" + "CHANGE_ME");
    }

    @Test
    void syncScript_restrictsStatusToConfiguredReaderGroup() throws Exception {
        String script = source("deploy/sync-backups-rclone.example.sh");

        assertThat(script).contains(
                "STATUS_FILE_GROUP=\"${STATUS_FILE_GROUP:-root}\"",
                "STATUS_FILE_MODE=\"${STATUS_FILE_MODE:-0600}\"",
                "getent group \"${STATUS_FILE_GROUP}\"",
                "chgrp -- \"${STATUS_FILE_GROUP}\"",
                "chmod -- \"${STATUS_FILE_MODE}\"");
        assertThat(script).doesNotContain("write_status \"SUCCESS\" || true");
    }

    @Test
    void orchestrator_scopesStatusPermissionsToEachBackup() throws Exception {
        String script = source("deploy/sync-offsite-backups.example.sh");

        assertThat(script).contains(
                "BUSINESS_STATUS_FILE_GROUP:-root",
                "BUSINESS_STATUS_FILE_MODE:-0600",
                "MES_STATUS_FILE_GROUP:-paper-mes",
                "MES_STATUS_FILE_MODE:-0640");
    }

    @Test
    void fallbackCron_preservesMesStatusPermissions() throws Exception {
        String cron = source("deploy/paper-mes-backup.cron.example");

        assertThat(cron).contains(
                "STATUS_FILE_GROUP=paper-mes STATUS_FILE_MODE=0640",
                "/usr/local/sbin/paper-mes-sync-rclone.sh");
    }

    @Test
    void qualityWorkflow_runsOffsiteBackupBehaviorAndContractTests() throws Exception {
        String workflow = source(".github/workflows/server-monitor-quality.yml");

        assertThat(workflow).contains(
                "deploy/*offsite*",
                "bash deploy/test-sync-backups-rclone.sh",
                "bash deploy/test-sync-offsite-backups.sh",
                "bash deploy/test-prune-offsite-backups.sh",
                "bash deploy/test-check-offsite-backup-capacity.sh",
                "HealthMonitoringContractTest,OffsiteBackupSyncContractTest");
    }

    @Test
    void deploymentGuide_installsCompleteOffsiteRuntime() throws Exception {
        String guide = source("docs/生产部署指南.md");

        assertThat(guide).contains(
                "/usr/local/sbin/paper-mes-sync-rclone.sh",
                "/usr/local/sbin/business-projects-sync-rclone.sh",
                "/usr/local/sbin/sync-offsite-backups",
                "/etc/systemd/system/paper-mes-offsite-backup.service",
                "systemctl start paper-mes-offsite-backup.service");
    }

    @Test
    void behaviorTest_coversSuccessfulAndFailedRemoteCopies() throws Exception {
        String script = source("deploy/test-sync-backups-rclone.sh");

        assertThat(script).contains("assert_status SUCCESS", "assert_status FAILED");
        assertThat(script).contains("test_remote:paper-mes-backups", "--checksum", "stat -c %a");
    }

    @Test
    void retentionScript_defaultsToDryRunAndUsesBoundedHardDeletes() throws Exception {
        String script = source("deploy/prune-offsite-backups.example.sh");

        assertThat(script).contains("OFFSITE_RETENTION_APPLY:-false", "MAX_DELETE_COUNT:-50");
        assertThat(script).contains("rclone purge", "--b2-hard-delete", "refusing to delete newest backup");
        assertThat(script).doesNotContain("rclone sync");
    }

    @Test
    void capacityScript_checksB2VersionsAndTransitionThresholds() throws Exception {
        String script = source("deploy/check-offsite-backup-capacity.example.sh");

        assertThat(script).contains("WARNING_BYTES:-8000000000", "CRITICAL_BYTES:-9000000000");
        assertThat(script).contains("--b2-versions", "previous_level", "RECOVERED");
    }

    @Test
    void offsiteBehaviorTestsCoverRetentionCapacityAndOrchestration() throws Exception {
        assertThat(source("deploy/test-prune-offsite-backups.sh"))
                .contains("monthly-permanent", "MAX_DELETE_COUNT", "unexpected-name");
        assertThat(source("deploy/test-check-offsite-backup-capacity.sh"))
                .contains("WARNING", "CRITICAL", "RECOVERED", "--b2-versions");
        assertThat(source("deploy/test-sync-offsite-backups.sh"))
                .contains("business mes retention capacity", "email:FAILED", "email:RECOVERED");
    }

    @Test
    void systemdTemplatesEnableRetentionAfterLocalCleanup() throws Exception {
        String service = source("deploy/paper-mes-offsite-backup.service.example");
        String timer = source("deploy/paper-mes-offsite-backup.timer.example");

        assertThat(service).contains("OFFSITE_RETENTION_APPLY=true", "DAILY_RETENTION_YEARS=3");
        assertThat(service).contains("WEEKLY_RETENTION_YEARS=5", "MAX_DELETE_COUNT=50");
        assertThat(service).contains("WARNING_BYTES=8000000000", "CRITICAL_BYTES=9000000000");
        assertThat(timer).contains("OnCalendar=*-*-* 03:30:00 Asia/Shanghai", "Persistent=true");
    }

    private String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }
}
